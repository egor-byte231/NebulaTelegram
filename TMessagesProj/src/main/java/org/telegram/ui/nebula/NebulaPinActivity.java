package org.telegram.ui.nebula;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.graphics.Color;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** PIN setup, change and removal. Requires old PIN to change or remove. */
public final class NebulaPinActivity extends Activity {
    private LinearLayout root;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28,40,28,28); root.setBackgroundColor(0xff100d1c);
        setContentView(root); render();
    }
    private EditText field(String hint) {
        EditText input = new EditText(this); input.setHint(hint); input.setTextColor(Color.WHITE);
        input.setHintTextColor(0xffb8abc9);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setSingleLine(true); return input;
    }
    private void render() {
        root.removeAllViews();
        TextView heading = new TextView(this); heading.setText("Захист Nebula PIN");
        heading.setTextColor(Color.WHITE); heading.setTextSize(24); root.addView(heading);
        TextView desc = new TextView(this);
        desc.setText("PIN захищає лише екрани Nebula. Не замінює пароль Telegram. Забутий PIN не відновлюється через цей екран.");
        desc.setTextColor(0xffb8abc9); root.addView(desc);
        Button change = new Button(this);
        change.setText(NebulaPin.enabled(this) ? "Змінити PIN" : "Створити PIN");
        change.setOnClickListener(v -> edit(false)); root.addView(change);
        if (NebulaPin.enabled(this)) {
            Button remove = new Button(this); remove.setText("Вимкнути захист PIN");
            remove.setOnClickListener(v -> edit(true)); root.addView(remove);
        }
    }
    private void edit(boolean remove) {
        LinearLayout fields = new LinearLayout(this); fields.setOrientation(LinearLayout.VERTICAL);
        boolean existing = NebulaPin.enabled(this);
        EditText oldPin = existing ? field("Поточний PIN") : null;
        if (oldPin != null) fields.addView(oldPin);
        EditText next = remove ? null : field("Новий PIN (4–12 цифр)");
        EditText confirm = remove ? null : field("Повторити новий PIN");
        if (next != null) { fields.addView(next); fields.addView(confirm); }
        new AlertDialog.Builder(this).setTitle(remove ? "Вимкнути PIN?" : "Зберегти PIN")
            .setView(fields).setNegativeButton("Скасувати",null)
            .setPositiveButton("Підтвердити", (d,w) -> {
                if (existing && !NebulaPin.verify(this, oldPin.getText().toString())) { error("Неправильний поточний PIN"); return; }
                if (remove) { NebulaPin.remove(this); render(); return; }
                String value = next.getText().toString();
                if (!NebulaPin.valid(value) || !value.equals(confirm.getText().toString())) { error("PIN має містити 4–12 цифр і збігатися з підтвердженням"); return; }
                if (!NebulaPin.set(this,value)) { error("Не вдалося зберегти PIN"); return; }
                render();
            }).show();
    }
    private void error(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
}
