package org.telegram.ui.nebula;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Map;
import java.util.Iterator;

/** Local, explicit snapshots of feature and global privacy preferences. No account secrets. */
public final class NebulaProfilesActivity extends NebulaProtectedActivity {
    private static final String PROFILE_PREFS = "nebula_profiles_v1";
    private static final String FEATURE_PREFS = "nebula_features_v1";
    private static final String PRIVACY_PREFS = "nebula_privacy_v1";
    private static final String STORAGE_KEY = "profiles";
    private static final int MAX_PROFILES = 20;
    private SharedPreferences profilesPrefs, featurePrefs, privacyPrefs;
    private JSONArray profiles;
    private LinearLayout list;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        profilesPrefs = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        featurePrefs = getSharedPreferences(FEATURE_PREFS, MODE_PRIVATE);
        privacyPrefs = getSharedPreferences(PRIVACY_PREFS, MODE_PRIVATE);
        try { profiles = new JSONArray(profilesPrefs.getString(STORAGE_KEY, "[]")); }
        catch (JSONException e) { profiles = new JSONArray(); }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 32, 24, 24);
        root.setBackgroundColor(0xff100d1c);
        TextView heading = new TextView(this);
        heading.setText("Nebula · Профілі налаштувань");
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(22);
        root.addView(heading);
        TextView info = new TextView(this);
        info.setText("Профілі зберігають локальні перемикачі Nebula Features і глобальні налаштування приватності. Правила окремих чатів, PIN, повідомлення та дані акаунтів не копіюються. Максимум 20 профілів.");
        info.setTextColor(0xffb8abc9);
        root.addView(info);
        Button save = new Button(this);
        save.setText("+ Зберегти поточні налаштування");
        save.setOnClickListener(v -> askName());
        root.addView(save);
        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        render();
    }

    private boolean persist() {
        return profilesPrefs.edit().putString(STORAGE_KEY, profiles.toString()).commit();
    }

    private static JSONObject snapshot(SharedPreferences preferences) {
        JSONObject values = new JSONObject();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (entry.getValue() instanceof Boolean) {
                try { values.put(entry.getKey(), entry.getValue()); }
                catch (JSONException ignored) { }
            }
        }
        return values;
    }

    private void askName() {
        if (profiles.length() >= MAX_PROFILES) {
            Toast.makeText(this, "Досягнуто ліміту профілів", Toast.LENGTH_LONG).show();
            return;
        }
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Назва профілю");
        new AlertDialog.Builder(this).setTitle("Зберегти профіль").setView(input)
            .setNegativeButton("Скасувати", null)
            .setPositiveButton("Зберегти", (d,w) -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty() || name.length() > 60) {
                    Toast.makeText(this, "Назва має містити від 1 до 60 символів", Toast.LENGTH_SHORT).show();
                    return;
                }
                JSONObject profile = new JSONObject();
                try {
                    profile.put("name", name);
                    profile.put("version", 2);
                    profile.put("values", snapshot(featurePrefs));
                    profile.put("privacy", snapshot(privacyPrefs));
                    profiles.put(profile);
                } catch (JSONException e) { return; }
                if (!persist()) {
                    profiles.remove(profiles.length() - 1);
                    Toast.makeText(this, "Не вдалося зберегти профіль", Toast.LENGTH_LONG).show();
                }
                render();
            }).show();
    }

    /** Only boolean settings are restored. No clear(): unrelated preferences survive. */
    private static boolean restoreBooleans(SharedPreferences preferences, JSONObject values) {
        if (values == null) return true;
        SharedPreferences.Editor editor = preferences.edit();
        Iterator<String> names = values.keys();
        while (names.hasNext()) {
            String key = names.next();
            Object value = values.opt(key);
            if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
        }
        return editor.commit();
    }

    private void restore(int index) {
        JSONObject profile = profiles.optJSONObject(index);
        if (profile == null) return;
        final JSONObject features = profile.optJSONObject("values");
        final JSONObject privacy = profile.optJSONObject("privacy");
        if (features == null) {
            Toast.makeText(this, "Профіль пошкоджено", Toast.LENGTH_LONG).show();
            return;
        }
        final String[] choices = {"Лише функції Nebula", "Лише глобальна приватність", "Обидва набори"};
        new AlertDialog.Builder(this).setTitle("Активувати профіль")
            .setItems(choices, (dialog, choice) -> {
                if (choice == 1 && privacy == null) {
                    Toast.makeText(this, "У старому профілі немає налаштувань приватності", Toast.LENGTH_LONG).show();
                    return;
                }
                String message = "Зміняться тільки збережені булеві перемикачі. Правила чатів, PIN та інші дані залишаться без змін. Продовжити?";
                new AlertDialog.Builder(this).setTitle("Підтвердження відновлення")
                    .setMessage(message).setNegativeButton("Скасувати", null)
                    .setPositiveButton("Активувати", (d,w) -> {
                        boolean ok = true;
                        if (choice != 1) ok = restoreBooleans(featurePrefs, features);
                        if (choice != 0) ok = restoreBooleans(privacyPrefs, privacy) && ok;
                        Toast.makeText(this, ok ? "Налаштування відновлено" : "Помилка збереження: перевірте налаштування", Toast.LENGTH_LONG).show();
                    }).show();
            }).show();
    }

    private void delete(int index) {
        new AlertDialog.Builder(this).setTitle("Видалити профіль?")
            .setNegativeButton("Скасувати", null)
            .setPositiveButton("Видалити", (d,w) -> {
                JSONArray previous = profiles;
                JSONArray next = new JSONArray();
                for (int i = 0; i < profiles.length(); i++) if (i != index) next.put(profiles.optJSONObject(i));
                profiles = next;
                if (!persist()) {
                    profiles = previous;
                    Toast.makeText(this, "Не вдалося видалити профіль", Toast.LENGTH_LONG).show();
                }
                render();
            }).show();
    }

    private void render() {
        list.removeAllViews();
        for (int i = 0; i < profiles.length(); i++) {
            final int index = i;
            JSONObject profile = profiles.optJSONObject(i);
            if (profile == null) continue;
            TextView title = new TextView(this);
            title.setText(profile.optString("name", "Профіль") + (profile.has("privacy") ? " · функції + приватність" : " · старий формат"));
            title.setTextSize(17);
            title.setTextColor(Color.WHITE);
            title.setPadding(0, 18, 0, 4);
            list.addView(title);
            Button activate = new Button(this);
            activate.setText("Активувати вибрані налаштування");
            activate.setOnClickListener(v -> restore(index));
            list.addView(activate);
            Button remove = new Button(this);
            remove.setText("Видалити");
            remove.setOnClickListener(v -> delete(index));
            list.addView(remove);
        }
    }
}
