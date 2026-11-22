package org.telegram.ui.nebula;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Local response templates: create, edit, delete, filter, and copy to clipboard. */
public final class NebulaTemplatesActivity extends NebulaProtectedActivity {
    private static final String PREFS = "nebula_templates_local_v1";
    private static final String KEY = "templates";
    private static final int MAX_TEMPLATES = 500, MAX_TITLE = 120, MAX_BODY = 20000;
    private final JSONArray templates = new JSONArray();
    private LinearLayout list;
    private EditText search;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff100d1c);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        try {
            JSONArray saved = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < saved.length(); i++) templates.put(saved.getJSONObject(i));
        } catch (JSONException ignored) { Toast.makeText(this, "Не вдалося прочитати шаблони", Toast.LENGTH_LONG).show(); }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(0xff100d1c);
        TextView title = new TextView(this);
        title.setText("Nebula · Шаблони відповідей");
        title.setTextSize(22); title.setTextColor(Color.WHITE);
        root.addView(title);
        TextView info = new TextView(this);
        info.setText("Локальні шаблони. Натисніть «Копіювати», щоб вставити текст у чат.");
        info.setTextColor(0xffb8abc9); root.addView(info);
        search = new EditText(this);
        search.setSingleLine(true); search.setHint("Пошук шаблонів");
        search.setTextColor(Color.WHITE); search.setHintTextColor(0xff9885ad);
        root.addView(search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            @Override public void afterTextChanged(Editable e) {}
        });
        Button add = new Button(this);
        add.setText("+ Новий шаблон"); add.setOnClickListener(v -> edit(-1));
        root.addView(add);
        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
        render();
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private boolean save() { return prefs.edit().putString(KEY, templates.toString()).commit(); }
    private void edit(int index) {
        JSONObject old = index < 0 ? null : templates.optJSONObject(index);
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL); fields.setPadding(dp(18), 0, dp(18), 0);
        EditText name = new EditText(this);
        name.setHint("Назва"); name.setSingleLine(true);
        name.setText(old == null ? "" : old.optString("title")); fields.addView(name);
        EditText body = new EditText(this);
        body.setHint("Текст відповіді"); body.setMinLines(4);
        body.setText(old == null ? "" : old.optString("body")); fields.addView(body);
        new AlertDialog.Builder(this).setTitle(index < 0 ? "Новий шаблон" : "Редагувати шаблон")
            .setView(fields).setNegativeButton("Скасувати", null)
            .setPositiveButton("Зберегти", (dialog, which) -> {
                String t = name.getText().toString().trim();
                String b = body.getText().toString().trim();
                if (t.isEmpty() || b.isEmpty() || t.length() > MAX_TITLE || b.length() > MAX_BODY) {
                    Toast.makeText(this, "Назва: 1–120, текст: 1–20000 символів", Toast.LENGTH_SHORT).show(); return;
                }
                if (index < 0 && templates.length() >= MAX_TEMPLATES) {
                    Toast.makeText(this, "Ліміт: 500 шаблонів", Toast.LENGTH_LONG).show(); return;
                }
                JSONObject obj = new JSONObject();
                try { obj.put("title", t); obj.put("body", b); } catch (JSONException e) { return; }
                if (index < 0) templates.put(obj); else {
                    try { templates.put(index, obj); } catch (JSONException e) { return; }
                }
                if (!save()) Toast.makeText(this, "Не вдалося зберегти шаблони", Toast.LENGTH_LONG).show();
                render();
            }).show();
    }
    private void render() {
        if (list == null) return;
        list.removeAllViews();
        String filter = search.getText().toString().toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < templates.length(); i++) {
            JSONObject obj = templates.optJSONObject(i);
            if (obj == null) continue;
            String title = obj.optString("title"); String body = obj.optString("body");
            if (!title.toLowerCase(java.util.Locale.ROOT).contains(filter) && !body.toLowerCase(java.util.Locale.ROOT).contains(filter)) continue;
            final int index = i;
            TextView item = new TextView(this);
            item.setText(title + "\n" + body);
            item.setTextColor(Color.WHITE); item.setTextSize(15);
            item.setPadding(dp(8), dp(12), dp(8), dp(6)); list.addView(item);
            LinearLayout actions = new LinearLayout(this);
            Button copy = new Button(this); copy.setText("Копіювати");
            copy.setOnClickListener(v -> NebulaPrivacyPolicy.confirmClipboard(this, body, () -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText(title, body));
                    Toast.makeText(this, "Скопійовано", Toast.LENGTH_SHORT).show();
                }
            })); actions.addView(copy);
            Button modify = new Button(this); modify.setText("Змінити");
            modify.setOnClickListener(v -> edit(index)); actions.addView(modify);
            Button remove = new Button(this); remove.setText("Видалити");
            remove.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("Видалити шаблон?")
                .setNegativeButton("Ні", null).setPositiveButton("Так", (d,w) -> {
                    templates.remove(index);
                    if (!save()) Toast.makeText(this, "Не вдалося зберегти зміни", Toast.LENGTH_LONG).show();
                    render();
                }).show()); actions.addView(remove);
            list.addView(actions);
        }
    }

}
