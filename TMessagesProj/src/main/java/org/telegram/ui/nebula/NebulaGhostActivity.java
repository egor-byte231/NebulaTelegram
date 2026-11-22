package org.telegram.ui.nebula;

import android.app.AlertDialog;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

/** Experimental local ghost controls; neither Telegram server nor other devices are controlled. */
public final class NebulaGhostActivity extends NebulaProtectedActivity {
    private Switch typing;
    private Switch reads;
    private Switch paused;
    private SharedPreferences preferences;
    private boolean syncing;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = NebulaToolUi.page(this, "Nebula · Режим привида (експеримент)",
            "Локальні налаштування двох часткових обмежень. Усі функції вимкнені за замовчуванням.");
        preferences = getSharedPreferences("nebula_privacy_v1", MODE_PRIVATE);
        paused = new Switch(this);
        paused.setText("Тимчасово призупинити всі обмеження режиму привида");
        paused.setTextColor(-1);
        paused.setChecked(preferences.getBoolean("ghost_paused", false));
        paused.setOnCheckedChangeListener((button, checked) -> {
            if (syncing) return;
            new AlertDialog.Builder(this)
                .setTitle(checked ? "Призупинити режим привида?" : "Відновити режим привида?")
                .setMessage(checked ? "Індивідуальні та глобальні правила залишаться збереженими, але більше не блокуватимуть ці два типи подій."
                    : "Збережені глобальні та індивідуальні правила знову діятимуть. Це не гарантує невидимості у Telegram.")
                .setNegativeButton("Скасувати", (d, w) -> syncSwitches())
                .setOnCancelListener(d -> syncSwitches())
                .setPositiveButton("Підтвердити", (d, w) -> saveOne("ghost_paused", checked))
                .show();
        });
        root.addView(paused);
        NebulaToolUi.text(this, root, "Тимчасова пауза автоматично завершується після вибраного часу, навіть після перезапуску застосунку. Постійна пауза має пріоритет.", 13, 0xffc8b8db);
        NebulaToolUi.button(this, root, "Пауза на 15 хвилин", () -> confirmTimedPause(15));
        NebulaToolUi.button(this, root, "Пауза на 1 годину", () -> confirmTimedPause(60));
        NebulaToolUi.button(this, root, "Скасувати тимчасову паузу", () -> confirmTimedPause(0));
        long remaining = NebulaGhostPolicy.remainingPauseMillis(this);
        if (remaining > 0L) {
            NebulaToolUi.text(this, root, "Тимчасова пауза: залишилося приблизно " + ((remaining + 59999L) / 60000L) + " хв.", 14, 0xffffc58d);
        }

        NebulaToolUi.text(this, root, "Пауза перекриває глобальні й індивідуальні правила, не видаляючи їх. Зміна діє для нових викликів, а не скасовує вже відправлені події.", 13, 0xffffc58d);
        typing = new Switch(this);
        typing.setText("Не надсилати стандартний індикатор друкування (усі чати)");
        typing.setTextColor(-1);
        typing.setChecked(preferences.getBoolean("suppress_typing", false));
        typing.setOnCheckedChangeListener((button, checked) -> { if (!syncing) saveOne("suppress_typing", checked); });
        root.addView(typing);
        reads = new Switch(this);
        reads.setText("Експериментально: не підтверджувати прочитання з відкритого чату");
        reads.setTextColor(-1);
        reads.setChecked(preferences.getBoolean("suppress_chat_read", false));
        reads.setOnCheckedChangeListener((button, checked) -> { if (!syncing) saveOne("suppress_chat_read", checked); });
        root.addView(reads);

        NebulaToolUi.text(this, root, "Швидкі профілі (впливають тільки на два основні перемикачі; паузу не змінюють):", 16, -1);
        NebulaToolUi.button(this, root, "Звичайний режим · усе вимкнено", () -> confirmPreset(false, false));
        NebulaToolUi.button(this, root, "Тихе друкування · лише індикатор", () -> confirmPreset(true, false));
        NebulaToolUi.button(this, root, "Експериментальний привид · обидва", () -> confirmPreset(true, true));

        int[] counts = NebulaGhostPresets.countChatExceptions(this);
        NebulaToolUi.text(this, root, "Окремі правила: друкування — " + counts[0] + ", прочитання — " + counts[1], 14, 0xffc8b8db);
        NebulaToolUi.button(this, root, "Скинути лише правила привида для всіх чатів", () ->
            new AlertDialog.Builder(this).setTitle("Скинути правила привида?")
                .setMessage("Видаляться лише індивідуальні налаштування друкування та прочитання. PIN, приховані чати та інші правила залишаться.")
                .setNegativeButton("Скасувати", null)
                .setPositiveButton("Скинути", (d, w) -> {
                    boolean ok = NebulaGhostPresets.clearChatExceptions(this);
                    Toast.makeText(this, ok ? "Правила привида скинуто" : "Не вдалося зберегти зміни", Toast.LENGTH_LONG).show();
                    if (ok) recreate();
                }).show());
        NebulaToolUi.button(this, root, "Правила для окремих чатів", () -> startActivity(new android.content.Intent(this, NebulaChatRulesActivity.class)));
        NebulaToolUi.button(this, root, "Перевірити поточні налаштування", () -> startActivity(new android.content.Intent(this, NebulaPrivacyAuditActivity.class)));
        NebulaToolUi.text(this, root,
            "Увага: приховуються лише окремі виклики ChatActivity. Це НЕ гарантує невидимості прочитання чи друкування. Інші екрани, пристрої та сервер Telegram можуть оновлювати статус. Онлайн-статус і перегляди історій не приховуються.",
            13, 0xffffc58d);
    }

    private void confirmTimedPause(int minutes) {
        new AlertDialog.Builder(this)
            .setTitle(minutes == 0 ? "Скасувати тимчасову паузу?" : "Призупинити режим привида?")
            .setMessage(minutes == 0 ? "Тимчасова пауза буде скасована. Постійний перемикач не зміниться."
                : "Експериментальні обмеження друкування та прочитання не діятимуть протягом " + minutes + " хвилин. Уже відправлені події не скасовуються.")
            .setNegativeButton("Скасувати", null)
            .setPositiveButton("Підтвердити", (d, w) -> {
                long until = minutes == 0 ? 0L : System.currentTimeMillis() + minutes * 60000L;
                boolean ok = preferences.edit().putLong("ghost_pause_until", until).commit();
                Toast.makeText(this, ok ? "Налаштування паузи збережено" : "Не вдалося зберегти паузу", Toast.LENGTH_LONG).show();
                if (ok) recreate();
            }).show();
    }

    private void saveOne(String key, boolean value) {
        if (!preferences.edit().putBoolean(key, value).commit()) {
            Toast.makeText(this, "Не вдалося зберегти налаштування", Toast.LENGTH_LONG).show();
            syncSwitches();
        }
    }

    private void syncSwitches() {
        syncing = true;
        paused.setChecked(preferences.getBoolean("ghost_paused", false));
        typing.setChecked(preferences.getBoolean("suppress_typing", false));
        reads.setChecked(preferences.getBoolean("suppress_chat_read", false));
        syncing = false;
    }

    private void confirmPreset(boolean type, boolean read) {
        new AlertDialog.Builder(this).setTitle("Змінити режим приватності?")
            .setMessage("Буде змінено тільки два глобальні перемикачі. Правила окремих чатів залишаться чинними. Ця функція експериментальна.")
            .setNegativeButton("Скасувати", null)
            .setPositiveButton("Застосувати", (d, w) -> {
                boolean ok = NebulaGhostPresets.apply(this, type, read);
                syncSwitches();
                Toast.makeText(this, ok ? "Профіль застосовано" : "Не вдалося зберегти профіль", Toast.LENGTH_LONG).show();
            }).show();
    }
}
