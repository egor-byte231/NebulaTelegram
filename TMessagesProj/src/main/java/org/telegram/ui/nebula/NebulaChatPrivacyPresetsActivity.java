package org.telegram.ui.nebula;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

/** Per-chat, local-only privacy presets. All changes are opt-in and atomic. */
public final class NebulaChatPrivacyPresetsActivity extends NebulaProtectedActivity {
    private int account;
    private long dialogId;
    private LinearLayout root;
    private static final String[] KEYS={"standard","screen","quiet","hidden"};
    private static final String[] LABELS={"Звичайний","Захист екрана","Тихий чат (експеримент)","Прихований чат (експеримент)"};
    private static final String[] DETAILS={
        "Вимикає чотири параметри приватності цього чату; PIN і архівування не змінюються.",
        "Увімкне захист скриншотів. Інші параметри цього набору будуть вимкнені.",
        "Захист екрана + експериментальне обмеження друкування та прочитання. Це не гарантує невидимості.",
        "Як тихий чат, але також приховує його зі звичайного списку діалогів. Пошук і сповіщення залишаються."
    };
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        if(!getIntent().hasExtra("nebula_dialog_id")){finish();return;}
        account=getIntent().getIntExtra("nebula_account",0);
        dialogId=getIntent().getLongExtra("nebula_dialog_id",0L);
        if(account<0||dialogId==0L){finish();return;}
        render();
    }
    private void render(){
        root=NebulaToolUi.page(this,"Nebula · Приватність чату","Профілі змінюють тільки локальні правила цього чату. PIN, нотатки, архів і чернетки залишаються без змін. Налаштування Telegram на сервері не змінюються.");
        NebulaToolUi.text(this,root,"Акаунт: "+account+" · Чат: "+dialogId,14,0xffc6a4ff);
        String[] keys={"secure_screen","suppress_typing","suppress_chat_read","hidden","pin_lock","exclude"};
        String[] names={"Захист екрана","Тихе друкування","Приховування прочитання","Прихований чат","PIN-захист","Виключено з архіву"};
        for(int i=0;i<keys.length;i++){
            boolean enabled=NebulaChatPolicy.enabled(this,account,dialogId,keys[i]);
            NebulaToolUi.text(this,root,names[i]+": "+(enabled?"увімкнено":"вимкнено"),14,enabled?0xffc6a4ff:0xffb4aabd);
        }
        for(int i=0;i<KEYS.length;i++){
            final int index=i;
            NebulaToolUi.button(this,root,LABELS[i],()->confirm(index));
        }
    }
    private void confirm(int index){
        new AlertDialog.Builder(this).setTitle(LABELS[index]).setMessage(DETAILS[index]+"\n\nЗастосувати профіль лише до цього чату?")
            .setNegativeButton("Скасувати",null)
            .setPositiveButton("Застосувати",(d,w)->{
                boolean ok=NebulaChatPolicy.applyPrivacyPreset(this,account,dialogId,KEYS[index]);
                Toast.makeText(this,ok?"Профіль збережено. Поверніться до чату для оновлення.":"Не вдалося зберегти",Toast.LENGTH_LONG).show();
                if(ok)render();
            }).show();
    }
}
