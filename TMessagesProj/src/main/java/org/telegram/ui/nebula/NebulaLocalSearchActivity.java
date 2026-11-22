package org.telegram.ui.nebula;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;
import java.util.Map;

/** Read-only unified search of Nebula-owned local data, not Telegram's message database. */
public final class NebulaLocalSearchActivity extends NebulaProtectedActivity {
    private static final int MAX_RESULTS = 100;
    private static final int MAX_SCAN = 1000;
    private LinearLayout results;
    private TextView status;
    private EditText query;
    private CheckBox notes, templates, drafts, bookmarks, rules, archive;
    private CheckBox deletedOnly, editedOnly;
    private int shown, scanned;
    private boolean truncated;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = NebulaToolUi.page(this, "Nebula · Розширений локальний пошук",
            "Пошук у локальних даних Nebula. Зашифрований архів повідомлень доступний окремою категорією, вимкненою за замовчуванням. Серверний пошук Telegram не використовується.");
        query = NebulaToolUi.edit(this, root, "Введіть щонайменше 2 символи");
        query.setSingleLine(true);
        notes = category(root, "Нотатки (загальні та для чатів)");
        templates = category(root, "Шаблони відповідей");
        drafts = category(root, "Чернетки");
        bookmarks = category(root, "Закладки");
        rules = category(root, "Правила чатів (лише назви та прапорці)");
        archive = category(root, "Зашифрований локальний архів повідомлень");
        archive.setChecked(false); // Sensitive content is never searched without explicit opt-in.
        deletedOnly = category(root, "Архів: лише позначені видаленими");
        editedOnly = category(root, "Архів: лише з історією редагувань");
        deletedOnly.setChecked(false);
        editedOnly.setChecked(false);
        deletedOnly.setVisibility(android.view.View.GONE);
        editedOnly.setVisibility(android.view.View.GONE);
        archive.setOnCheckedChangeListener((button, checked) -> {
            deletedOnly.setVisibility(checked ? android.view.View.VISIBLE : android.view.View.GONE);
            editedOnly.setVisibility(checked ? android.view.View.VISIBLE : android.view.View.GONE);
            render();
        });
        status = NebulaToolUi.text(this, root, "", 13, 0xffc6a4ff);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        root.addView(results);
        query.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            @Override public void afterTextChanged(Editable e) { }
        });
        render();
    }

    private CheckBox category(LinearLayout root, String label) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextColor(0xffffffff);
        box.setChecked(true);
        box.setOnCheckedChangeListener((button, checked) -> render());
        root.addView(box);
        return box;
    }

    private boolean eligible(String content, String term) {
        return content != null && content.toLowerCase(Locale.ROOT).contains(term);
    }

    private void add(String category, String key, String content, String term, Class<?> destination) {
        if (scanned++ >= MAX_SCAN) { truncated = true; return; }
        if (!eligible(key + "\n" + content, term)) return;
        if (shown >= MAX_RESULTS) { truncated = true; return; }
        shown++;
        String safeContent = content.length() > 400 ? content.substring(0, 400) + "…" : content;
        TextView item = NebulaToolUi.text(this, results,
            category + (key.isEmpty() ? "" : " · " + key) + "\n" + safeContent,
            15, 0xffffffff);
        item.setOnClickListener(v -> startActivity(new Intent(this, destination)));
    }

    private JSONArray array(String pref, String key) {
        try { return new JSONArray(getSharedPreferences(pref, MODE_PRIVATE).getString(key, "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    private void searchArchive(String term) {
        if (!archive.isChecked() || scanned >= MAX_SCAN || shown >= MAX_RESULTS) return;
        try {
            java.util.List<NebulaMessageArchive.Entry> entries = NebulaMessageArchive.load(this);
            // Search most recent snapshots first; the same bounded result budget applies.
            for (int i = entries.size() - 1; i >= 0; i--) {
                if (scanned >= MAX_SCAN || shown >= MAX_RESULTS) {
                    truncated = true;
                    break;
                }
                NebulaMessageArchive.Entry e = entries.get(i);
                if (deletedOnly.isChecked() && !e.deleted) continue;
                if (editedOnly.isChecked() && e.versions.size() < 2) continue;
                String latest = e.versions.isEmpty() ? "" : e.versions.get(e.versions.size() - 1);
                // Check every stored edit version, not only the latest visible one.
                boolean match = eligible(e.chatKey, term) || String.valueOf(e.messageId).contains(term);
                for (String version : e.versions) {
                    if (eligible(version, term)) { match = true; break; }
                }
                scanned++;
                if (!match) continue;
                shown++;
                String preview = latest.length() > 400 ? latest.substring(0, 400) + "…" : latest;
                TextView item = NebulaToolUi.text(this, results,
                    "Архів · " + e.chatKey + " · #" + e.messageId
                    + (e.deleted ? " · видалено" : "")
                    + (e.versions.size() > 1 ? " · версій: " + e.versions.size() : "")
                    + "\n" + preview, 15, 0xffffffff);
                final String searchTerm = term;
                final String chatKey = e.chatKey;
                item.setOnClickListener(v -> {
                    Intent intent = new Intent(this, NebulaArchiveActivity.class);
                    intent.putExtra("nebula_search", searchTerm);
                    intent.putExtra("nebula_chat_key", chatKey);
                    startActivity(intent);
                });
            }
        } catch (Exception ex) {
            NebulaToolUi.text(this, results,
                "Не вдалося відкрити зашифрований архів. Перевірте локальне сховище.",
                13, 0xffffc58d);
        }
    }

    private void render() {
        if (results == null || query == null) return;
        results.removeAllViews();
        shown = scanned = 0;
        truncated = false;
        String term = query.getText().toString().trim().toLowerCase(Locale.ROOT);
        if (term.length() < 2) {
            status.setText("Введіть щонайменше 2 символи для пошуку.");
            return;
        }
        if (notes.isChecked()) {
            String scratch = getSharedPreferences("nebula_notes_local_v1", MODE_PRIVATE).getString("scratchpad", "");
            add("Загальна нотатка", "", scratch, term, NebulaNotesActivity.class);
            SharedPreferences chatNotes = getSharedPreferences("nebula_chat_notes_v1", MODE_PRIVATE);
            for (Map.Entry<String, ?> entry : chatNotes.getAll().entrySet()) {
                if (scanned >= MAX_SCAN) { truncated = true; break; }
                if (entry.getValue() instanceof String)
                    add("Нотатка чату", entry.getKey(), (String) entry.getValue(), term, NebulaChatNotesActivity.class);
            }
        }
        if (templates.isChecked()) {
            JSONArray a = array("nebula_templates_local_v1", "templates");
            for (int i = 0; i < a.length() && scanned < MAX_SCAN; i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null) add("Шаблон", o.optString("title"), o.optString("body"), term, NebulaTemplatesActivity.class);
            }
            if (a.length() > MAX_SCAN - scanned) truncated = true;
        }
        if (drafts.isChecked()) {
            JSONArray a = array("nebula_drafts_v1", "items");
            for (int i = 0; i < a.length() && scanned < MAX_SCAN; i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null) add("Чернетка", o.optString("chat"), o.optString("text"), term, NebulaDraftsActivity.class);
            }
            if (a.length() > MAX_SCAN - scanned) truncated = true;
        }
        if (bookmarks.isChecked()) {
            JSONArray a = array("nebula_bookmarks_v1", "items");
            for (int i = 0; i < a.length() && scanned < MAX_SCAN; i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null) add("Закладка", o.optString("title"), o.optString("url"), term, NebulaBookmarksActivity.class);
            }
            if (a.length() > MAX_SCAN - scanned) truncated = true;
        }
        if (rules.isChecked()) {
            SharedPreferences chatRules = getSharedPreferences("nebula_chat_rules_v1", MODE_PRIVATE);
            for (Map.Entry<String, ?> entry : chatRules.getAll().entrySet()) {
                if (scanned >= MAX_SCAN) { truncated = true; break; }
                if (!(entry.getValue() instanceof String)) continue;
                try {
                    JSONObject o = new JSONObject((String) entry.getValue());
                    StringBuilder enabled = new StringBuilder();
                    for (String key : new String[]{"hidden", "exclude", "secure_screen", "pin_lock", "suppress_typing", "suppress_chat_read"}) {
                        if (o.optBoolean(key, false)) enabled.append(key).append(" ");
                    }
                    add("Правило чату", entry.getKey(), enabled.toString(), term, NebulaChatRulesActivity.class);
                } catch (Exception ignored) { }
            }
        }
        searchArchive(term);
        status.setText("Знайдено: " + shown + (truncated ? " (частину результатів пропущено через обмеження пошуку)" : "")
            + ". Натисніть результат, щоб відкрити відповідний менеджер.");
        if (shown == 0) NebulaToolUi.text(this, results, "Збігів не знайдено.", 14, 0xffb8abc9);
    }
}
