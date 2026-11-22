package org.telegram.ui.nebula;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import org.json.JSONObject;
import java.util.Map;

/** Read-only overview of local privacy settings and real feature availability. */
public final class NebulaPrivacyAuditActivity extends NebulaProtectedActivity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = NebulaToolUi.page(this, "Nebula · Перевірка приватності",
            "Показано налаштування цього пристрою. Це не перевірка Telegram-серверів.");
        SharedPreferences features = getSharedPreferences("nebula_features_v1", MODE_PRIVATE);
        SharedPreferences privacy = getSharedPreferences("nebula_privacy_v1", MODE_PRIVATE);
        boolean secure = features.getBoolean("privacy_2", false);
        NebulaToolUi.text(this, root, "Захист скриншотів у всьому застосунку: " + (secure ? "УВІМКНЕНО" : "ВИМКНЕНО"), 16, -1);
        NebulaToolUi.text(this, root, "PIN інструментів Nebula: " + (NebulaPin.enabled(this) ? "НАЛАШТОВАНО" : "НЕ НАЛАШТОВАНО"), 16, -1);
        NebulaToolUi.text(this, root, "Блокування після згортання: " + (privacy.getBoolean("lock_on_background", false) ? "УВІМКНЕНО" : "ВИМКНЕНО"), 16, -1);
        SharedPreferences rules = getSharedPreferences("nebula_chat_rules_v1", MODE_PRIVATE);
        int secured = 0, excluded = 0, hidden = 0;
        for (Map.Entry<String, ?> entry : rules.getAll().entrySet()) {
            if (!entry.getKey().startsWith("telegram:") || !(entry.getValue() instanceof String)) continue;
            try {
                JSONObject item = new JSONObject((String) entry.getValue());
                if (item.optBoolean("secure_screen", false)) secured++;
                if (item.optBoolean("exclude", false)) excluded++;
                if (item.optBoolean("hidden", false)) hidden++;
            } catch (Exception ignored) {}
        }
        NebulaToolUi.text(this, root, "Чатів із захистом скриншотів: " + secured, 16, -1);
        NebulaToolUi.text(this, root, "Чатів, виключених з архівування: " + excluded, 16, -1);
        NebulaToolUi.text(this, root, "Локальних позначок прихованих чатів: " + hidden, 16, -1);
        NebulaToolUi.text(this, root, "Приховані чати вилучаються зі звичайного списку, але можуть залишатися у пошуку, сповіщеннях та інших розділах.", 13, 0xffb8abc9);
        NebulaToolUi.text(this, root, "Приватність друкування та прочитання реалізована лише для окремих викликів ChatActivity. Онлайн-статус і перегляди історій не приховуються.", 13, 0xffb8abc9);
        NebulaToolUi.text(this, root, "Режим привида призупинено: " + (privacy.getBoolean("ghost_paused", false) ? "ТАК — обмеження не застосовуються" : "НІ"), 16, 0xffffc58d);
        NebulaToolUi.text(this, root, "Приховування друкування (глобально): " + (privacy.getBoolean("suppress_typing", false) ? "УВІМКНЕНО" : "ВИМКНЕНО"), 15, -1);
        NebulaToolUi.text(this, root, "Пропуск підтверджень прочитання (глобально): " + (privacy.getBoolean("suppress_chat_read", false) ? "УВІМКНЕНО" : "ВИМКНЕНО"), 15, -1);
        int typingRules=0, readRules=0, pinRules=0;
        for (Map.Entry<String, ?> entry : rules.getAll().entrySet()) {
            if (!entry.getKey().startsWith("telegram:") || !(entry.getValue() instanceof String)) continue;
            try {
                JSONObject item=new JSONObject((String)entry.getValue());
                if(item.optBoolean("suppress_typing",false)) typingRules++;
                if(item.optBoolean("suppress_chat_read",false)) readRules++;
                if(item.optBoolean("pin_lock",false)) pinRules++;
            } catch(Exception ignored) {}
        }
        NebulaToolUi.text(this, root, "Чатів із правилом друкування: " + typingRules + " · прочитання: " + readRules + " · PIN: " + pinRules, 15, -1);
        NebulaToolUi.text(this, root, "Важливо: глобальне вимкнення не скасовує активних правил окремих чатів; окрема пауза перекриває обидва типи правил. Профілі привида не змінюють індивідуальні правила.", 13, 0xffffc58d);
        NebulaToolUi.button(this, root, "Керувати профілями привида", () -> startActivity(new android.content.Intent(this, NebulaGhostActivity.class)));
        NebulaToolUi.button(this, root, "Оновити перевірку", this::recreate);
    }
}
