package org.telegram.ui.nebula;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/** PIN-protected local index of chats tagged hidden. This does not hide Telegram's native dialog rows. */
public final class NebulaHiddenChatsActivity extends NebulaProtectedActivity {
    private static final String PREF = "nebula_chat_rules_v1";
    private LinearLayout entries;
    private EditText search;
    private boolean onlyHidden = true;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = NebulaToolUi.page(this, "Nebula · Приховані чати",
            "Позначені чати прибираються зі звичайного списку Telegram. Вони можуть залишатися в пошуку, сповіщеннях та інших екранах. Для захисту цього менеджера налаштуйте PIN Nebula.");
        if (!NebulaPin.enabled(this)) NebulaToolUi.text(this, root,
            "Увага: PIN Nebula не налаштований. Позначки доступні кожному, хто відкриє цей екран.", 14, 0xffffb16d);
        search = NebulaToolUi.edit(this, root, "Пошук за ID чату або акаунтом");
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { render(); }
            public void afterTextChanged(Editable e) {}
        });
        NebulaToolUi.button(this, root, "Показати приховані / усі позначені чати", () -> {
            onlyHidden = !onlyHidden; render();
        });
        NebulaToolUi.button(this, root, "Додати чат за ID", this::addById);
        entries = new LinearLayout(this); entries.setOrientation(LinearLayout.VERTICAL);
        root.addView(entries);
        render();
    }
    @Override protected void onResume() { super.onResume(); if (entries != null) render(); }

    private void addById() {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        EditText accountInput = new EditText(this); accountInput.setHint("Номер акаунта (0, 1, …)");
        accountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); form.addView(accountInput);
        EditText idInput = new EditText(this); idInput.setHint("Числовий ID діалогу (може бути від’ємним)");
        idInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        form.addView(idInput);
        new AlertDialog.Builder(this).setTitle("Приховати чат у звичайному списку")
            .setView(form).setNegativeButton("Скасувати",null)
            .setPositiveButton("Додати", (d,w) -> {
                try {
                    int account = Integer.parseInt(accountInput.getText().toString().trim());
                    long id = Long.parseLong(idInput.getText().toString().trim());
                    if (account < 0 || account > 9 || id == 0) throw new NumberFormatException();
                    if (!NebulaChatPolicy.set(this,account,id,"hidden",true)) throw new IllegalStateException();
                    render();
                } catch (Exception e) { Toast.makeText(this,"Некоректний ID або помилка збереження",Toast.LENGTH_LONG).show(); }
            }).show();
    }
    private void render() {
        if (entries == null) return;
        entries.removeAllViews();
        SharedPreferences prefs = getSharedPreferences(PREF, Context.MODE_PRIVATE);
        ArrayList<String> keys = new ArrayList<>(prefs.getAll().keySet());
        Collections.sort(keys);
        String needle = search == null ? "" : search.getText().toString().toLowerCase(Locale.ROOT).trim();
        int count=0;
        for (String key : keys) {
            // Ignore free-form labels: only account-scoped numeric dialog keys can control real chats.
            String[] parts=key.split(":",-1);
            if (parts.length != 3 || !"telegram".equals(parts[0])) continue;
            final int account; final long dialogId;
            try { account=Integer.parseInt(parts[1]); dialogId=Long.parseLong(parts[2]); }
            catch (NumberFormatException e) { continue; }
            if (account < 0 || account > 9 || dialogId == 0) continue;
            JSONObject policy=NebulaChatPolicy.read(this,account,dialogId);
            boolean hidden=policy.optBoolean("hidden",false);
            if (onlyHidden && !hidden) continue;
            if (!key.toLowerCase(Locale.ROOT).contains(needle)) continue;
            count++;
            NebulaToolUi.text(this,entries,"Акаунт "+account+" · чат "+dialogId+
                (hidden?" · прихований у списку":" · звичайний"),15,0xffffffff);
            NebulaToolUi.button(this,entries,hidden?"Зняти позначку прихованого":"Позначити прихованим", () -> {
                if (NebulaChatPolicy.set(this,account,dialogId,"hidden",!hidden)) render();
                else Toast.makeText(this,"Не вдалося зберегти",Toast.LENGTH_SHORT).show();
            });
            NebulaToolUi.button(this,entries,"Відкрити локальний центр чату", () -> {
                android.content.Intent intent=new android.content.Intent(this,NebulaChatHubActivity.class);
                intent.putExtra("nebula_account",account);intent.putExtra("nebula_dialog_id",dialogId);
                startActivity(intent);
            });
            NebulaToolUi.button(this,entries,"Копіювати ID", () ->
                NebulaPrivacyPolicy.confirmClipboard(this,String.valueOf(dialogId),() -> {
                    ClipboardManager manager=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    if(manager!=null)manager.setPrimaryClip(ClipData.newPlainText("Chat ID",String.valueOf(dialogId)));
                }));
        }
        if(count==0)NebulaToolUi.text(this,entries,"Немає чатів із такими локальними позначками.",14,0xffb8abc9);
    }
}
