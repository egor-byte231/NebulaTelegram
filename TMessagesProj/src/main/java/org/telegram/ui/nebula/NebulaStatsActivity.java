package org.telegram.ui.nebula;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

/** Local-only statistics; never claims to represent all Telegram messages. */
public final class NebulaStatsActivity extends NebulaProtectedActivity {
    private LinearLayout root;
    private boolean chatOnly;
    private int account;
    private long dialogId;
    private String chatKey;

    private static final class Counts {
        int archive, deleted, edited, versions, drafts, due, notes, rules, bookmarks;
        long textCharacters;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Intent intent = getIntent();
        chatOnly = intent.hasExtra("nebula_account") && intent.hasExtra("nebula_dialog_id");
        account = intent.getIntExtra("nebula_account", 0);
        dialogId = intent.getLongExtra("nebula_dialog_id", 0L);
        chatKey = NebulaChatPolicy.key(account, dialogId);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (root != null) render();
    }

    private JSONArray array(String pref, String field) {
        try { return new JSONArray(getSharedPreferences(pref, 0).getString(field, "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    private boolean matches(int a, long id) { return !chatOnly || (a == account && id == dialogId); }

    private void render() {
        root = NebulaToolUi.page(this, chatOnly ? "Nebula · Статистика чату" : "Nebula · Локальна статистика",
            "Лише дані, збережені Nebula на цьому пристрої. Це НЕ повна статистика Telegram. Дані не надсилаються в мережу.");
        if (chatOnly) NebulaToolUi.text(this, root, "Акаунт " + account + " · чат " + dialogId, 15, 0xffc6a4ff);
        Counts c = new Counts();
        try {
            List<NebulaMessageArchive.Entry> entries = NebulaMessageArchive.load(this);
            for (NebulaMessageArchive.Entry entry : entries) {
                if (entry == null || (chatOnly && !chatKey.equals(entry.chatKey))) continue;
                c.archive++;
                if (entry.deleted) c.deleted++;
                if (entry.versions.size() > 1) c.edited++;
                c.versions += entry.versions.size();
                for (String version : entry.versions) if (version != null) c.textCharacters += version.length();
            }
        } catch (RuntimeException e) {
            NebulaToolUi.text(this, root, "Архів не вдалося прочитати. Перевірте ключ шифрування або стан сховища.", 14, 0xffffb38a);
        }
        JSONArray drafts = array("nebula_drafts_v1", "items");
        long now = System.currentTimeMillis();
        for (int i = 0; i < drafts.length(); i++) {
            JSONObject d = drafts.optJSONObject(i);
            if (d == null || !matches(d.optInt("account", -1), d.optLong("dialog_id", Long.MIN_VALUE))) continue;
            c.drafts++;
            long time = d.optLong("time", 0);
            if (time > 0 && time <= now) c.due++;
        }
        if (chatOnly) {
            String note = getSharedPreferences("nebula_chat_notes_v1", 0).getString(chatKey, "");
            c.notes = note.isEmpty() ? 0 : 1;
            c.rules = NebulaChatPolicy.read(this, account, dialogId).length();
        } else {
            Map<String, ?> notes = getSharedPreferences("nebula_chat_notes_v1", 0).getAll();
            for (Object value : notes.values()) if (value instanceof String && !((String)value).isEmpty()) c.notes++;
            Map<String, ?> rules = getSharedPreferences("nebula_chat_rules_v1", 0).getAll();
            c.rules = rules.size();
        }
        NebulaToolUi.text(this, root,
            "Знімків повідомлень: " + c.archive +
            "\nПозначено видаленими: " + c.deleted +
            "\nЗ історією редагувань: " + c.edited +
            "\nЗбережених версій: " + c.versions +
            "\nСимволів в архіві (всі версії): " + c.textCharacters +
            "\nЧернеток: " + c.drafts +
            "\nЧернеток із датою, що настала: " + c.due +
            "\nНотаток чатів: " + c.notes +
            (chatOnly ? "\nПараметрів правил чату: " : "\nЗаписів правил чатів: ") + c.rules,
            16, 0xffffffff);
        if (chatOnly) {
            NebulaToolUi.text(this, root, "PIN-захист: " + yes("pin_lock") +
                "\nПрихований у списку: " + yes("hidden") +
                "\nЗахист екрана: " + yes("secure_screen") +
                "\nВиключений з архіву: " + yes("exclude") +
                "\nПриховування друкування: " + yes("suppress_typing") +
                "\nЕкспериментальне прочитання: " + yes("suppress_chat_read"), 15, 0xffc6a4ff);
            NebulaToolUi.button(this, root, "Відкрити архів цього чату", () -> open(NebulaArchiveActivity.class));
            NebulaToolUi.button(this, root, "Відкрити чернетки цього чату", () -> open(NebulaDraftsActivity.class));
            NebulaToolUi.button(this, root, "Відкрити правила цього чату", () -> open(NebulaChatRulesActivity.class));
        } else {
            File dir = new File(getFilesDir(), "nebula_attachments");
            File[] files = dir.listFiles();
            int count = 0; long bytes = 0;
            if (files != null) for (File file : files) if (file.isFile()) { count++; bytes += file.length(); }
            NebulaToolUi.text(this, root, "Локальних вкладень: " + count + "\nОбсяг вкладень: " + (bytes / 1024) + " КБ", 15, 0xffc6a4ff);
            NebulaToolUi.button(this, root, "Відкрити архів", () -> open(NebulaArchiveActivity.class));
            NebulaToolUi.button(this, root, "Відкрити планувальник чернеток", () -> open(NebulaDraftsActivity.class));
        }
        NebulaToolUi.button(this, root, "Оновити статистику", this::render);
    }

    private String yes(String rule) {
        return NebulaChatPolicy.enabled(this, account, dialogId, rule) ? "так" : "ні";
    }

    private void open(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        if (chatOnly) {
            intent.putExtra("nebula_account", account);
            intent.putExtra("nebula_dialog_id", dialogId);
        }
        startActivity(intent);
    }
}
