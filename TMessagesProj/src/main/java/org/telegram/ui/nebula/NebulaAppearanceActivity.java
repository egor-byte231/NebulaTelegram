package org.telegram.ui.nebula;

import android.os.Bundle;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

/** Local appearance controls; stars are applied to the main dialogs header. */
public final class NebulaAppearanceActivity extends NebulaProtectedActivity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = NebulaToolUi.page(this, "Оформлення Nebula",
            "Зірочки застосовуються до заголовка списку чатів після повторного відкриття екрана. Інші параметри оформлення — експериментальні та ще не змінюють усі екрани Telegram.");
        SharedPreferences p = getSharedPreferences("nebula_appearance_v1", 0);
        NebulaToolUi.text(this, root, "Швидкі профілі", 17, -1);
        preset(root, "Вимкнути всі ефекти", "off");
        preset(root, "Nebula: фіолетові зірки", "nebula");
        preset(root, "Контрастно, без анімацій", "accessible");
        String[] keys = {"stars", "animations", "compact", "pill", "violet", "reduced_motion", "high_contrast"};
        String[] labels = {"Зірочки в заголовку чатів", "Анімація зірочок", "Компактний список (планується)", "Pill Stack (планується)", "Фіолетові акценти (частково)", "Менше руху", "Підвищена контрастність зірочок"};
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            Switch sw = new Switch(this);
            sw.setText(labels[i]); sw.setTextColor(-1);
            sw.setChecked(p.getBoolean(key, false));
            sw.setOnCheckedChangeListener((view, on) -> {
                if (!p.edit().putBoolean(key, on).commit()) {
                    sw.setOnCheckedChangeListener(null);
                    sw.setChecked(!on);
                    sw.setOnCheckedChangeListener((v, checked) -> p.edit().putBoolean(key, checked).apply());
                    Toast.makeText(this, "Не вдалося зберегти налаштування", Toast.LENGTH_SHORT).show();
                }
            });
            root.addView(sw);
        }
        NebulaToolUi.text(this, root, "Інтенсивність зірочок", 17, -1);
        SeekBar bar = new SeekBar(this); bar.setMax(100);
        bar.setProgress(NebulaAppearancePolicy.intensity(this));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int value, boolean fromUser) { }
            public void onStartTrackingTouch(SeekBar b) { }
            public void onStopTrackingTouch(SeekBar b) {
                if (!p.edit().putInt("intensity", b.getProgress()).commit())
                    Toast.makeText(NebulaAppearanceActivity.this, "Не вдалося зберегти інтенсивність", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(bar);
        NebulaToolUi.button(this, root, "Скинути оформлення", () -> {
            if (p.edit().clear().commit()) recreate();
            else Toast.makeText(this, "Помилка скидання", Toast.LENGTH_SHORT).show();
        });
    }
    private void preset(LinearLayout root, String label, String id) {
        NebulaToolUi.button(this, root, label, () -> {
            if (NebulaAppearancePolicy.preset(this, id)) recreate();
            else Toast.makeText(this, "Не вдалося застосувати профіль", Toast.LENGTH_SHORT).show();
        });
    }
}
