package org.telegram.ui.nebula;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;

/** User-created local bookmarks, scoped to a Telegram account and optionally a chat. */
public final class NebulaBookmarksActivity extends NebulaProtectedActivity {
    private static final String PREF = "nebula_bookmarks_v1";
    private static final int MAX_ITEMS = 500;
    private LinearLayout root, list;
    private EditText search, title, url;
    private int account;
    private long dialogId;
    private boolean chatOnly;

    private JSONArray load() {
        try { return new JSONArray(getSharedPreferences(PREF, MODE_PRIVATE).getString("items", "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }
    private boolean save(JSONArray items) {
        return getSharedPreferences(PREF, MODE_PRIVATE).edit().putString("items", items.toString()).commit();
    }
    private boolean belongs(JSONObject item) {
        if (item == null) return false;
        // Older entries were global. They remain visible in the general manager only.
        if (!item.has("account")) return !chatOnly;
        if (item.optInt("account", -1) != account && chatOnly) return false;
        return !chatOnly || item.optLong("dialog_id", Long.MIN_VALUE) == dialogId;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        account = getIntent().getIntExtra("nebula_account", 0);
        chatOnly = getIntent().hasExtra("nebula_dialog_id");
        dialogId = getIntent().getLongExtra("nebula_dialog_id", 0L);
        root = NebulaToolUi.page(this, "Nebula · Локальні закладки",
            chatOnly ? "Закладки цього чату (акаунт " + account + ", ID " + dialogId + "). Локально, без синхронізації." :
            "Власні локальні посилання. Закладки не є закладками повідомлень Telegram і не синхронізуються.");
        title = NebulaToolUi.edit(this, root, "Назва закладки");
        url = NebulaToolUi.edit(this, root, "HTTPS-посилання");
        NebulaToolUi.button(this, root, "Додати закладку", () -> add());
        search = NebulaToolUi.edit(this, root, "Пошук за назвою або URL");
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) { render(); }
            @Override public void afterTextChanged(android.text.Editable e) {}
        });
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list);
        render();
    }
    private static boolean validUrl(String value) {
        if (value.length() > 2048 || !value.startsWith("https://")) return false;
        try {
            android.net.Uri u = android.net.Uri.parse(value);
            return "https".equals(u.getScheme()) && u.getHost() != null && !u.getHost().isEmpty()
                && u.getUserInfo() == null;
        } catch (Exception e) { return false; }
    }
    private void add() {
        String t = title.getText().toString().trim(), u = url.getText().toString().trim();
        if (t.isEmpty() || t.length() > 120 || !validUrl(u)) { notice("Назва: 1–120 символів; URL: коректне HTTPS-посилання без логіна й пароля"); return; }
        JSONArray items = load(); if (items.length() >= MAX_ITEMS) { notice("Ліміт: 500 закладок"); return; }
        try {
            JSONObject o = new JSONObject(); o.put("title", t); o.put("url", u);
            o.put("account", account); if (chatOnly) o.put("dialog_id", dialogId);
            o.put("created_at", System.currentTimeMillis()); items.put(o);
            if (!save(items)) { notice("Не вдалося зберегти"); return; }
            title.setText(""); url.setText(""); render();
        } catch (JSONException e) { notice("Помилка даних"); }
    }
    private void render() {
        if (list == null) return;
        list.removeAllViews(); JSONArray items = load();
        String q = search == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int shown = 0;
        for (int i = items.length() - 1; i >= 0; i--) {
            JSONObject o = items.optJSONObject(i); if (!belongs(o)) continue;
            String t = o.optString("title"), u = o.optString("url");
            if (!q.isEmpty() && !t.toLowerCase(Locale.ROOT).contains(q) && !u.toLowerCase(Locale.ROOT).contains(q)) continue;
            shown++; final int index = i;
            NebulaToolUi.text(this, list, t + "\n" + u, 15, 0xffeeeeff);
            NebulaToolUi.button(this, list, "Відкрити", () -> {
                if (!validUrl(u)) { notice("Неприпустиме посилання"); return; }
                try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(u))); }
                catch (Exception e) { notice("Немає застосунку для відкриття посилання"); }
            });
            NebulaToolUi.button(this, list, "Редагувати", () -> edit(index));
            NebulaToolUi.button(this, list, "Видалити", () -> new AlertDialog.Builder(this)
                .setTitle("Видалити закладку?").setNegativeButton("Скасувати", null)
                .setPositiveButton("Видалити", (d,w) -> remove(index)).show());
        }
        if (shown == 0) NebulaToolUi.text(this, list, "Закладок не знайдено", 14, 0xffb8abc9);
        else NebulaToolUi.text(this, list, "Показано: " + shown, 13, 0xffb8abc9);
    }
    private void edit(int index) {
        JSONArray items = load(); JSONObject old = items.optJSONObject(index);
        if (old == null || !belongs(old)) { notice("Запис не знайдено"); return; }
        LinearLayout fields = new LinearLayout(this); fields.setOrientation(LinearLayout.VERTICAL);
        EditText t = new EditText(this), u = new EditText(this);
        t.setSingleLine(true); t.setText(old.optString("title"));
        u.setSingleLine(true); u.setText(old.optString("url"));
        fields.addView(t); fields.addView(u);
        new AlertDialog.Builder(this).setTitle("Редагувати закладку").setView(fields)
            .setNegativeButton("Скасувати", null).setPositiveButton("Зберегти", (d,w) -> {
                String name = t.getText().toString().trim(), link = u.getText().toString().trim();
                if (name.isEmpty() || name.length() > 120 || !validUrl(link)) { notice("Некоректна назва або HTTPS-посилання"); return; }
                try { old.put("title", name); old.put("url", link); old.put("updated_at", System.currentTimeMillis());
                    if (!save(items)) notice("Не вдалося зберегти"); else render();
                } catch (JSONException e) { notice("Помилка редагування"); }
            }).show();
    }
    private void remove(int index) {
        JSONArray items = load(), next = new JSONArray();
        for (int i = 0; i < items.length(); i++) if (i != index) next.put(items.opt(i));
        if (!save(next)) notice("Не вдалося видалити"); else render();
    }
    private void notice(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
