package org.telegram.ui.nebula;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Editor for local rules and opt-in masking of matching plain-text chat rows. */
public final class NebulaFilterActivity extends NebulaProtectedActivity {
    private LinearLayout root, list;
    private EditText sample, chat;
    private String scopedChatKey = "";
    @Override public void onCreate(Bundle b){super.onCreate(b);
        if (getIntent().hasExtra("nebula_dialog_id")) {
            int account=getIntent().getIntExtra("nebula_account",0);
            long dialogId=getIntent().getLongExtra("nebula_dialog_id",0L);
            scopedChatKey="telegram:"+account+":"+dialogId;
        }
        root=NebulaToolUi.page(this,"Фільтри повідомлень","Правила працюють у локальному тесті та в ручній перевірці завантажених повідомлень із меню звичайного чату. Можна окремо ввімкнути локальне маскування текстових повідомлень у звичайних чатах. Воно не видаляє повідомлення із Telegram.");
        CheckBox autoMask = new CheckBox(this);
        autoMask.setText("Автоматично маскувати текст за правилами (експериментально)");
        autoMask.setTextColor(0xFFFFFFFF);
        autoMask.setChecked(NebulaAutoMask.enabled(this));
        root.addView(autoMask);
        autoMask.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                new AlertDialog.Builder(this)
                    .setTitle("Локальне маскування повідомлень")
                    .setMessage("Приховуватиметься лише текст звичайних повідомлень, що збігається з активними правилами. Це не видаляє повідомлення на сервері. Вкладення, сповіщення, пошук і пересилання можуть показувати оригінал. Зміни видно після повторного відкриття чату.")
                    .setNegativeButton("Скасувати", (d, w) -> autoMask.setChecked(false))
                    .setPositiveButton("Увімкнути", (d, w) -> {
                        if (!NebulaAutoMask.setEnabled(this, true)) {
                            autoMask.setChecked(false);
                            Toast.makeText(this, "Помилка збереження", Toast.LENGTH_LONG).show();
                        }
                    }).show();
            } else if (!NebulaAutoMask.setEnabled(this, false)) {
                Toast.makeText(this, "Помилка збереження", Toast.LENGTH_LONG).show();
            }
        });
        if (!scopedChatKey.isEmpty()) {
            final int scopedAccount = getIntent().getIntExtra("nebula_account", 0);
            final long scopedDialog = getIntent().getLongExtra("nebula_dialog_id", 0L);
            CheckBox excludeChat = new CheckBox(this);
            excludeChat.setText("Не маскувати повідомлення в цьому чаті");
            excludeChat.setTextColor(0xFFFFFFFF);
            excludeChat.setChecked(NebulaAutoMask.isChatExcluded(this, scopedAccount, scopedDialog));
            root.addView(excludeChat);
            excludeChat.setOnCheckedChangeListener((button, checked) -> {
                if (!NebulaAutoMask.setChatExcluded(this, scopedAccount, scopedDialog, checked)) {
                    excludeChat.setOnCheckedChangeListener(null);
                    excludeChat.setChecked(!checked);
                    excludeChat.setOnCheckedChangeListener((b, v) -> {
                        if (!NebulaAutoMask.setChatExcluded(this, scopedAccount, scopedDialog, v))
                            Toast.makeText(this, "Помилка збереження", Toast.LENGTH_LONG).show();
                    });
                    Toast.makeText(this, "Помилка збереження", Toast.LENGTH_LONG).show();
                }
            });
            NebulaToolUi.text(this, root,
                "Виняток застосовується тільки до цього акаунта й чату. Зміни видно після повторного відкриття чату.",
                13, 0xFFBBAADD);
        }
        NebulaToolUi.button(this,root,"Переглянути винятки маскування",()->showMaskExclusions());
        if (!scopedChatKey.isEmpty()) {
            NebulaToolUi.button(this,root,"Увімкнути правила цього чату",()->bulkScoped(true));
            NebulaToolUi.button(this,root,"Вимкнути правила цього чату",()->bulkScoped(false));
        }
        NebulaToolUi.button(this,root,"+ Створити правило",()->edit(null));
        NebulaToolUi.button(this,root,"Увімкнути всі правила",()->bulkEnable(true));
        NebulaToolUi.button(this,root,"Вимкнути всі правила",()->bulkEnable(false));
        NebulaToolUi.button(this,root,"Експортувати правила (JSON)",()->exportRules());
        NebulaToolUi.button(this,root,"Імпортувати правила (JSON)",()->importRules());
        sample=NebulaToolUi.edit(this,root,"Текст повідомлення для перевірки");
        chat=NebulaToolUi.edit(this,root,"Ключ чату (необов'язково)");
        if (!scopedChatKey.isEmpty()) { chat.setText(scopedChatKey); chat.setEnabled(false); }
        Spinner sampleDirection=new Spinner(this);
        sampleDirection.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Напрямок невідомий", "Вхідне повідомлення", "Вихідне повідомлення"}));
        root.addView(sampleDirection);
        NebulaToolUi.button(this,root,"Перевірити всі активні правила",()->{
            List<NebulaFilterEngine.Rule> hits=new ArrayList<>();
            for(NebulaFilterEngine.Rule rule:NebulaFilterEngine.forChat(this,chat.getText().toString().trim()))
                if(NebulaFilterEngine.matches(rule,sample.getText().toString(),chat.getText().toString().trim(),sampleDirection.getSelectedItemPosition())) hits.add(rule);
            Toast.makeText(this,"Збігів: "+hits.size(),Toast.LENGTH_LONG).show();
            new AlertDialog.Builder(this).setTitle("Результат перевірки")
                .setMessage(hits.isEmpty()?"Немає збігів":TextUtils.join("\n",names(hits)))
                .setPositiveButton("OK",null).show();
        });
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);render();
    }
    private List<String> names(List<NebulaFilterEngine.Rule> rules){List<String> out=new ArrayList<>();for(NebulaFilterEngine.Rule r:rules)out.add("• "+r.phrase);return out;}
    private void edit(NebulaFilterEngine.Rule old){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        EditText phrase=new EditText(this);phrase.setHint("Слово або regex");phrase.setSingleLine(false);phrase.setText(old==null?"":old.phrase);form.addView(phrase);
        EditText key=new EditText(this);key.setHint("Ключ чату; порожній = усі чати");key.setText(old==null?"":old.chatKey);form.addView(key);
        if (!scopedChatKey.isEmpty()) {key.setText(scopedChatKey);key.setEnabled(false);}
        CheckBox regex=new CheckBox(this);regex.setText("Регулярний вираз");regex.setChecked(old!=null&&old.regex);form.addView(regex);
        CheckBox ignore=new CheckBox(this);ignore.setText("Ігнорувати регістр");ignore.setChecked(old==null||old.ignoreCase);form.addView(ignore);
        Spinner direction=new Spinner(this);
        String[] directions={"Усі повідомлення", "Лише вхідні", "Лише вихідні"};
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,directions);
        direction.setAdapter(adapter); direction.setSelection(old==null?0:Math.max(0,Math.min(2,old.direction)));
        form.addView(direction);
        CheckBox enabled=new CheckBox(this);enabled.setText("Правило активне");enabled.setChecked(old==null||old.enabled);form.addView(enabled);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(old==null?"Нове правило":"Редагувати правило")
            .setView(form).setNegativeButton("Скасувати",null).setPositiveButton("Зберегти",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{
            NebulaFilterEngine.Rule r=new NebulaFilterEngine.Rule(old==null?UUID.randomUUID().toString():old.id,
                phrase.getText().toString().trim(),key.getText().toString().trim(),regex.isChecked(),ignore.isChecked(),enabled.isChecked());
            r.direction=direction.getSelectedItemPosition();
            try{NebulaFilterEngine.validate(r);}catch(Exception ex){phrase.setError("Некоректне правило: "+ex.getMessage());return;}
            List<NebulaFilterEngine.Rule> rules=NebulaFilterEngine.load(this);boolean replaced=false;
            for(int i=0;i<rules.size();i++)if(rules.get(i).id.equals(r.id)){rules.set(i,r);replaced=true;break;}
            if(!replaced)rules.add(r);
            if(!NebulaFilterEngine.save(this,rules)){Toast.makeText(this,"Не вдалося зберегти",Toast.LENGTH_LONG).show();return;}
            dialog.dismiss();render();
        }));dialog.show();
    }
    private void render(){if(list==null)return;list.removeAllViews();List<NebulaFilterEngine.Rule> rules=NebulaFilterEngine.load(this);
        if (!scopedChatKey.isEmpty()) {
            ArrayList<NebulaFilterEngine.Rule> filtered = new ArrayList<>();
            for (NebulaFilterEngine.Rule r : rules) if (r.chatKey.isEmpty() || scopedChatKey.equals(r.chatKey)) filtered.add(r);
            rules=filtered;
        }
        NebulaToolUi.text(this,list,"Правил: "+rules.size(),16,0xffffffff);
        for(NebulaFilterEngine.Rule r:rules){
            NebulaToolUi.text(this,list,(r.enabled?"● ":"○ ")+r.phrase+(r.regex?" [regex]":"")+(r.direction==1?" [вхідні]":r.direction==2?" [вихідні]":"")+(r.chatKey.isEmpty()?"":" · "+r.chatKey),15,0xffffffff);
            NebulaToolUi.button(this,list,"Редагувати",()->edit(r));
            NebulaToolUi.button(this,list,"Дублювати",()->{
                NebulaFilterEngine.Rule copy=new NebulaFilterEngine.Rule(UUID.randomUUID().toString(),r.phrase,r.chatKey,r.regex,r.ignoreCase,r.enabled);
                copy.direction=r.direction;
                List<NebulaFilterEngine.Rule> all=NebulaFilterEngine.load(this);all.add(copy);
                if(NebulaFilterEngine.save(this,all))render();
            });
            NebulaToolUi.button(this,list,r.enabled?"Вимкнути":"Увімкнути",()->{
                List<NebulaFilterEngine.Rule> all=NebulaFilterEngine.load(this);for(NebulaFilterEngine.Rule x:all)if(x.id.equals(r.id))x.enabled=!x.enabled;
                if(NebulaFilterEngine.save(this,all))render();
            });
            NebulaToolUi.button(this,list,"Видалити",()->new AlertDialog.Builder(this).setMessage("Видалити правило?")
                .setNegativeButton("Скасувати",null).setPositiveButton("Видалити",(d,w)->{
                    List<NebulaFilterEngine.Rule> all=NebulaFilterEngine.load(this);for(int i=all.size()-1;i>=0;i--)if(all.get(i).id.equals(r.id))all.remove(i);
                    if(NebulaFilterEngine.save(this,all))render();
                }).show());
        }
    }
    /** Changes only rules explicitly bound to this chat; global rules remain untouched. */
    private void bulkScoped(boolean enabled) {
        if (scopedChatKey.isEmpty()) return;
        new AlertDialog.Builder(this).setTitle(enabled?"Увімкнути правила чату?":"Вимкнути правила чату?")
            .setMessage("Буде змінено лише правила, прив'язані до цього чату. Глобальні правила залишаться без змін.")
            .setNegativeButton("Скасувати",null).setPositiveButton("Підтвердити",(d,w)->{
                List<NebulaFilterEngine.Rule> rules=NebulaFilterEngine.load(this);
                int count=0;
                for (NebulaFilterEngine.Rule r:rules) if (scopedChatKey.equals(r.chatKey)) {r.enabled=enabled;count++;}
                if (count==0) {Toast.makeText(this,"Немає правил цього чату",Toast.LENGTH_SHORT).show();return;}
                if (NebulaFilterEngine.save(this,rules)) render();
                else Toast.makeText(this,"Помилка збереження",Toast.LENGTH_LONG).show();
            }).show();
    }
    private void showMaskExclusions() {
        java.util.List<String> entries=NebulaAutoMask.excludedChats(this);
        if (entries.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Винятки маскування")
                .setMessage("Немає чатів, виключених із маскування.").setPositiveButton("OK",null).show();
            return;
        }
        String[] items=new String[entries.size()];
        for (int i=0;i<items.length;i++) items[i]="Акаунт і чат: "+entries.get(i);
        new AlertDialog.Builder(this).setTitle("Винятки маскування ("+entries.size()+")")
            .setItems(items,(d,which)->{
                String entry=entries.get(which);
                new AlertDialog.Builder(this).setTitle("Прибрати виняток?")
                    .setMessage("Знову дозволити локальне маскування для "+entry+"? Зміни видно після повторного відкриття чату.")
                    .setNegativeButton("Скасувати",null).setPositiveButton("Прибрати",(confirm,w)->{
                        String[] parts=entry.split(":",2);
                        try {
                            int account=Integer.parseInt(parts[0]); long dialog=Long.parseLong(parts[1]);
                            if (NebulaAutoMask.setChatExcluded(this,account,dialog,false)) showMaskExclusions();
                            else Toast.makeText(this,"Помилка збереження",Toast.LENGTH_LONG).show();
                        } catch (RuntimeException ex) {Toast.makeText(this,"Некоректний ключ чату",Toast.LENGTH_LONG).show();}
                    }).show();
            })
            .setNeutralButton("Очистити всі",(d,w)->new AlertDialog.Builder(this)
                .setTitle("Очистити всі винятки?")
                .setMessage("Всі чати знову підпадатимуть під глобальні правила маскування. Самі правила не зміняться.")
                .setNegativeButton("Скасувати",null)
                .setPositiveButton("Очистити",(confirm,button)->{
                    if (!NebulaAutoMask.clearChatExclusions(this)) Toast.makeText(this,"Помилка збереження",Toast.LENGTH_LONG).show();
                }).show())
            .setNegativeButton("Закрити",null).show();
    }
    private void bulkEnable(boolean enabled) {
        new AlertDialog.Builder(this).setTitle(enabled?"Увімкнути всі?":"Вимкнути всі?")
            .setMessage("Змінити стан усіх локальних правил, включно з правилами інших чатів?")
            .setNegativeButton("Скасувати",null)
            .setPositiveButton("Підтвердити",(d,w)->{
                if(NebulaFilterEngine.setAllEnabled(this,enabled))render();
                else Toast.makeText(this,"Помилка збереження",Toast.LENGTH_LONG).show();
            }).show();
    }
    private void exportRules() {
        try {
            String json=NebulaFilterEngine.exportJson(this);
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Nebula filter backup",json));
            Toast.makeText(this,"JSON скопійовано в буфер обміну",Toast.LENGTH_LONG).show();
        } catch(Exception e) {Toast.makeText(this,"Помилка експорту",Toast.LENGTH_LONG).show();}
    }
    private void importRules() {
        EditText input=new EditText(this);input.setHint("Вставте JSON резервної копії");input.setMinLines(5);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Імпорт правил (заміна всіх)")
            .setMessage("Імпорт замінить усі поточні правила. Перед цим експортуйте резервну копію.")
            .setView(input).setNegativeButton("Скасувати",null).setPositiveButton("Імпортувати",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{
            try {
                if(!NebulaFilterEngine.importJson(this,input.getText().toString()))throw new IllegalStateException("Storage failure");
                dialog.dismiss();render();Toast.makeText(this,"Правила імпортовано",Toast.LENGTH_SHORT).show();
            } catch(Exception e) {input.setError("Помилка: "+e.getMessage());}
        }));dialog.show();
    }

}
