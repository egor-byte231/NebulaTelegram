package org.telegram.ui.nebula;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;

/** Nebula feature preferences UI. Switches are intentionally not connected to Telegram internals yet. */
public final class NebulaFeaturesActivity extends NebulaProtectedActivity {
    private static final int BG = Color.rgb(14, 11, 22);
    private static final int CARD = Color.rgb(31, 24, 45);
    private static final int VIOLET = Color.rgb(176, 126, 255);
    private SharedPreferences preferences;
    private LinearLayout content;
    private final java.util.ArrayList<SwitchEntry> switches = new java.util.ArrayList<>();
    private static final class SwitchEntry { final Switch view; final String title; final LinearLayout section; SwitchEntry(Switch v, String t, LinearLayout s) {view=v;title=t;section=s;} }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("nebula_features_v1", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int margin = dp(18);
        content.setPadding(margin, dp(28), margin, dp(32));
        scroll.addView(content);
        setContentView(scroll);
        addHeading("Nebula Features", 27, Color.WHITE);
        addDescription("Налаштування додаткових функцій. Більшість перемикачів поки не змінюють поведінку Telegram.");
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Пошук налаштувань…");
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(0xffb8abc9);
        content.addView(search);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { filterSwitches(s.toString()); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        Button reset = new Button(this);
        reset.setText("Скинути перемикачі Nebula");
        reset.setAllCaps(false);
        reset.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
            .setTitle("Скинути налаштування?")
            .setMessage("Усі перемикачі буде вимкнено. Нотатки, шаблони, файли й PIN залишаться без змін.")
            .setNegativeButton("Скасувати", null)
            .setPositiveButton("Скинути", (dialog, which) -> {
                preferences.edit().clear().commit();
                for (SwitchEntry e : switches) e.view.setChecked(false);
                android.widget.Toast.makeText(this, "Перемикачі скинуто", android.widget.Toast.LENGTH_SHORT).show();
            }).show());
        content.addView(reset);
        addDescription("Центр чату доступний у меню відкритого чату Telegram: нотатки, правила, чернетки та локальні дії.");
        addToolShortcut("Правила та виключення для чатів", NebulaChatRulesActivity.class);
        addToolShortcut("Менеджер локально прихованих чатів", NebulaHiddenChatsActivity.class);
        addToolShortcut("Центр приватності", NebulaPrivacyCenterActivity.class);
        addToolShortcut("Експериментальне приховування друкування", NebulaGhostActivity.class);
        addToolShortcut("Перевірка приватності та захисту чатів", NebulaPrivacyAuditActivity.class);
        addToolShortcut("Оформлення та анімації", NebulaAppearanceActivity.class);
        addToolShortcut("Швидкі жести Nebula", NebulaGesturesActivity.class);
        addToolShortcut("Контроль локальних даних", NebulaDataControlActivity.class);
        addToolShortcut("Локальний архів повідомлень", NebulaArchiveActivity.class);
        addToolShortcut("Нотатки за чатами", NebulaChatNotesActivity.class);
        addToolShortcut("Локальні закладки", NebulaBookmarksActivity.class);
        addToolShortcut("Інструменти тексту", NebulaTextToolsActivity.class);
        addToolShortcut("Експорт та імпорт даних", NebulaBackupActivity.class);
        addToolShortcut("Захист PIN для інструментів Nebula", NebulaPinActivity.class);
        addToolShortcut("Пошук у локальних даних", NebulaLocalSearchActivity.class);
        addToolShortcut("Локальні вкладення та ліміт", NebulaStorageActivity.class);
        addToolShortcut("Менеджер HTTPS-завантажень", NebulaDownloadsActivity.class);
        addToolShortcut("Локальна статистика", NebulaStatsActivity.class);
        addNotesShortcut();
        addTemplatesShortcut();
        addProfilesShortcut();
        addToolShortcut("Планувальник чернеток", NebulaDraftsActivity.class);
        addToolShortcut("Фільтри: декілька правил і перевірка", NebulaFilterActivity.class);
        addSection("Пошук і повідомлення", new String[]{"Розширений пошук","Локальні нотатки","Шаблони відповідей","Планувальник чернеток","Відкладене надсилання","Надсилання без звуку","Автопозначення прочитаних"}, "messages");
        addSection("Приватність", new String[]{"Пароль на чати","Приховані чати","Приватний екран","Режим привида","Читання без позначки","Історії без позначки","Приховувати онлайн","Приховувати друкування","Автоматичний офлайн","Запитувати перед історіями"}, "privacy");
        addSection("Архів повідомлень", new String[]{"Зберігати видалені","Історія редагувань","Виключення архівування","Зберігати вкладення","Ліміт вкладень","Дата прочитання","Останній час онлайн","Напівпрозорі видалені","Позначка видалено"}, "archive");
        addSection("Фільтри", new String[]{"Ключові слова та regex","Загальні фільтри","Приховати заблокованих","Локальний тіньовий бан"}, "filters");
        addSection("Інтерфейс", new String[]{"Профілі налаштувань","Швидкі жести","Налаштування навігації","Верхнє меню","Бокова панель","Pill Stack","Подвійний дотик","Свайпи повідомлень","Індикатор привида","3D-ефекти","Літаючі зірочки"}, "interface");
        addSection("Система", new String[]{"Менеджер завантажень","Статистика чатів","Економія трафіку","Власний push","Приховування реклами"}, "system");
    }
    private void addToolShortcut(String label, Class<? extends Activity> activity) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false);
        button.setOnClickListener(v -> startActivity(new Intent(this, activity))); content.addView(button);
    }
    private void addNotesShortcut() {
        Button notes = new Button(this);
        notes.setText("Відкрити локальні нотатки");
        notes.setAllCaps(false);
        notes.setOnClickListener(v -> startActivity(new Intent(this, NebulaNotesActivity.class)));
        content.addView(notes);
    }
    private void addTemplatesShortcut() {
        Button templates = new Button(this);
        templates.setText("Відкрити шаблони відповідей");
        templates.setAllCaps(false);
        templates.setOnClickListener(v -> startActivity(new Intent(this, NebulaTemplatesActivity.class)));
        content.addView(templates);
    }
    private void addProfilesShortcut() {
        Button profiles = new Button(this);
        profiles.setText("Профілі налаштувань");
        profiles.setAllCaps(false);
        profiles.setOnClickListener(v -> startActivity(new Intent(this, NebulaProfilesActivity.class)));
        content.addView(profiles);
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void addHeading(String s, int size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setPadding(0,dp(8),0,dp(8)); content.addView(t);
    }
    private void addDescription(String s) {
        TextView t = new TextView(this); t.setText(s); t.setTextColor(0xffb8abc9);t.setTextSize(13);
        t.setPadding(0,0,0,dp(16)); content.addView(t);
    }
    private void addSection(String title, String[] options, String prefix) {
        addHeading(title, 19, VIOLET);
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable shape = new GradientDrawable(); shape.setColor(CARD);shape.setCornerRadius(dp(18));
        card.setBackground(shape); card.setPadding(dp(12),dp(8),dp(12),dp(8));
        for(int i=0;i<options.length;i++) {
            final String key = prefix + "_" + i;
            Switch toggle = new Switch(this);
            toggle.setText(options[i]); toggle.setTextColor(Color.WHITE); toggle.setTextSize(14);
            toggle.setPadding(dp(4),dp(9),dp(4),dp(9));
            toggle.setChecked(preferences.getBoolean(key,false));
            toggle.setOnCheckedChangeListener((button, checked) -> preferences.edit().putBoolean(key, checked).apply());
            card.addView(toggle);
            switches.add(new SwitchEntry(toggle, options[i], card));
        }
        content.addView(card);
    }

    private void filterSwitches(String query) {
        String needle = query.trim().toLowerCase(java.util.Locale.ROOT);
        for (SwitchEntry entry : switches) {
            entry.view.setVisibility(entry.title.toLowerCase(java.util.Locale.ROOT).contains(needle) ? View.VISIBLE : View.GONE);
        }
        java.util.HashSet<LinearLayout> sections = new java.util.HashSet<>();
        for (SwitchEntry entry : switches) sections.add(entry.section);
        for (LinearLayout section : sections) {
            boolean visible = false;
            for (SwitchEntry entry : switches) if (entry.section == section && entry.view.getVisibility() == View.VISIBLE) { visible = true; break; }
            section.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
