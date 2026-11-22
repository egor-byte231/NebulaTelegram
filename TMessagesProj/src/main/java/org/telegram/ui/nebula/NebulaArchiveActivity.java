package org.telegram.ui.nebula;

import android.app.AlertDialog;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Read-only browser for snapshots explicitly collected on device. */
public final class NebulaArchiveActivity extends NebulaProtectedActivity {
    private static final int EXPORT_REQUEST = 4182;
    private LinearLayout root,items;
    private boolean deletedOnly = false;
    private boolean editedOnly = false;
    private boolean matchingRulesOnly = false;
    private boolean unmatchedRulesOnly = false;
    private boolean matchAnyVersion = true;
    private int recentDays = 0;
    private boolean newestFirst = true;
    private int accountFilter = -1;
    private String chatFilter = null;
    private EditText search;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        root=NebulaToolUi.page(this,"Архів повідомлень","Лише локально отримані та збережені повідомлення. Архівування вимкнене за замовчуванням.");
        if (getIntent().hasExtra("nebula_dialog_id") && getIntent().hasExtra("nebula_account")) {
            int account = getIntent().getIntExtra("nebula_account", -1);
            long dialogId = getIntent().getLongExtra("nebula_dialog_id", 0L);
            if (account >= 0 && dialogId != 0L) {
                chatFilter = "telegram:" + account + ":" + dialogId;
                accountFilter = account;
            }
        }
        // Read-only navigation from unified search; never changes archiving preferences.
        String linkedChat = getIntent().getStringExtra("nebula_chat_key");
        if (linkedChat != null && linkedChat.matches("telegram:[0-9]+:-?[0-9]+")) {
            chatFilter = linkedChat;
        }
        // The archive preferences are the same keys used by the Nebula Features switches.
        android.content.SharedPreferences archiveFlags = getSharedPreferences("nebula_features_v1", MODE_PRIVATE);
        android.widget.Switch saveDeleted = new android.widget.Switch(this);
        saveDeleted.setText("Зберігати отримані повідомлення для історії видалень");
        saveDeleted.setTextColor(0xffffffff);
        saveDeleted.setChecked(NebulaMessageArchive.capturesDeleted(this));
        saveDeleted.setOnCheckedChangeListener((v, on) -> {
            archiveFlags.edit().putBoolean("archive_0", on).apply();
            render();
        });
        root.addView(saveDeleted);
        android.widget.Switch saveEdits = new android.widget.Switch(this);
        saveEdits.setText("Зберігати історію редагувань (до 12 версій)");
        saveEdits.setTextColor(0xffffffff);
        saveEdits.setChecked(NebulaMessageArchive.capturesEdits(this));
        saveEdits.setOnCheckedChangeListener((v, on) -> {
            archiveFlags.edit().putBoolean("archive_1", on).apply();
            render();
        });
        root.addView(saveEdits);
        NebulaToolUi.text(this,root,"Зберігаються лише повідомлення, які клієнт встиг отримати. Вимкнення не видаляє старі копії — для цього є очищення архіву.",13,0xffb8abc9);
        search=NebulaToolUi.edit(this,root,"Пошук за текстом або ID чату");
        String initialSearch = getIntent().getStringExtra("nebula_search");
        if (initialSearch != null && initialSearch.length() <= 200) search.setText(initialSearch);
        search.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int f){}
            public void onTextChanged(CharSequence s,int a,int b,int c){render();}
            public void afterTextChanged(android.text.Editable e){}
        });
        if (chatFilter != null) {
            NebulaToolUi.text(this,root,"Лише цей чат: " + chatFilter,14,0xffc6a4ff);
            NebulaToolUi.button(this,root,"Показати всі чати",()->{chatFilter=null;accountFilter=-1;render();});
        }
        NebulaToolUi.button(this,root,"Призупинити / відновити архівування",()->{
            boolean pause = !NebulaMessageArchive.isPaused(this);
            NebulaMessageArchive.setPaused(this,pause);
            Toast.makeText(this,pause?"Архівування призупинено":"Архівування відновлено",Toast.LENGTH_SHORT).show();
            render();
        });
        NebulaToolUi.button(this,root,"Фільтр за акаунтом",()->{
            java.util.TreeSet<Integer> ids = new java.util.TreeSet<>();
            for (NebulaMessageArchive.Entry entry : NebulaMessageArchive.load(this)) {
                int id = NebulaMessageArchive.accountOf(entry);
                if (id >= 0) ids.add(id);
            }
            java.util.ArrayList<Integer> choices = new java.util.ArrayList<>();
            choices.add(-1); choices.addAll(ids);
            String[] names = new String[choices.size()];
            for (int i=0;i<choices.size();i++) names[i]=choices.get(i)<0?"Усі акаунти":"Акаунт "+choices.get(i);
            new AlertDialog.Builder(this).setTitle("Показувати повідомлення")
                .setItems(names,(dialog,which)->{accountFilter=choices.get(which);render();})
                .setNegativeButton("Скасувати",null).show();
        });
        NebulaToolUi.button(this,root,"Очистити архів вибраного акаунта",()->{
            if(accountFilter<0){Toast.makeText(this,"Спочатку виберіть акаунт",Toast.LENGTH_SHORT).show();return;}
            new AlertDialog.Builder(this).setTitle("Видалити копії акаунта "+accountFilter+"?")
                .setMessage("Це видалить усі локальні копії повідомлень вибраного акаунта.")
                .setNegativeButton("Скасувати",null)
                .setPositiveButton("Видалити",(d,w)->{NebulaMessageArchive.clearAccount(this,accountFilter);render();}).show();
        });
        NebulaToolUi.button(this,root,"Очистити весь архів",()->new AlertDialog.Builder(this)
            .setTitle("Видалити локальний архів?")
            .setMessage("Усі локальні копії повідомлень і версій буде видалено без можливості відновлення.")
            .setNegativeButton("Скасувати",null)
            .setPositiveButton("Видалити",(d,w)->{NebulaMessageArchive.clear(this);render();}).show());
        NebulaToolUi.button(this,root,"Термін зберігання",()->new AlertDialog.Builder(this)
            .setTitle("Автоматичне очищення архіву")
            .setSingleChoiceItems(new String[]{"7 днів","30 днів","90 днів","Без обмеження часу"},
                retentionChoice(),(dialog,which)->{
                    NebulaMessageArchive.setRetentionDays(this,new int[]{7,30,90,0}[which]);
                    dialog.dismiss();render();
                }).setNegativeButton("Скасувати",null).show());
        NebulaToolUi.button(this,root,"Ліміт повідомлень",()->new AlertDialog.Builder(this)
            .setTitle("Максимум локальних копій")
            .setSingleChoiceItems(new String[]{"250","1000","2000"},entryChoice(),(dialog,which)->{
                NebulaMessageArchive.setEntryLimit(this,new int[]{250,1000,2000}[which]);
                dialog.dismiss();render();
            }).setNegativeButton("Скасувати",null).show());
        NebulaToolUi.button(this,root,"Показати лише видалені: ні",()->{
            deletedOnly=!deletedOnly;
            if(deletedOnly)editedOnly=false;
            render();
        });
        NebulaToolUi.button(this,root,"Показати лише редаговані: ні",()->{
            editedOnly=!editedOnly;
            if(editedOnly)deletedOnly=false;
            render();
        });
        NebulaToolUi.button(this,root,"Показати лише збіги з фільтрами",()->{
            matchingRulesOnly=!matchingRulesOnly;
            if(matchingRulesOnly)unmatchedRulesOnly=false;
            render();
        });
        NebulaToolUi.button(this,root,"Показати лише повідомлення без збігів",()->{
            unmatchedRulesOnly=!unmatchedRulesOnly;
            if(unmatchedRulesOnly)matchingRulesOnly=false;
            render();
        });
        NebulaToolUi.button(this,root,"Перевіряти всі збережені версії",()->{
            matchAnyVersion=!matchAnyVersion;
            render();
        });
        NebulaToolUi.button(this,root,"Період відображення",()->new AlertDialog.Builder(this)
            .setTitle("Показувати повідомлення за період")
            .setSingleChoiceItems(new String[]{"За весь час","Останні 24 години","Останні 7 днів","Останні 30 днів"},
                recentDays==0?0:recentDays==1?1:recentDays==7?2:3,
                (dialog,which)->{recentDays=new int[]{0,1,7,30}[which];dialog.dismiss();render();})
            .setNegativeButton("Скасувати",null).show());
        NebulaToolUi.button(this,root,"Порядок: спочатку нові / старі",()->{
            newestFirst=!newestFirst;render();
        });
        NebulaToolUi.button(this,root,"Скинути фільтри перегляду",()->{
            deletedOnly=false;editedOnly=false;matchingRulesOnly=false;unmatchedRulesOnly=false;
            matchAnyVersion=true;recentDays=0;accountFilter=-1;chatFilter=null;
            newestFirst=true;search.setText("");render();
        });
        NebulaToolUi.button(this,root,"Експортувати архів у JSON",()->new AlertDialog.Builder(this)
            .setTitle("Експорт приватних повідомлень")
            .setMessage("Файл міститиме текст повідомлень та історію редагувань у відкритому вигляді. Зберігайте його лише в безпечному місці.")
            .setNegativeButton("Скасувати",null)
            .setPositiveButton("Вибрати місце",(d,w)->startExport()).show());
        items=new LinearLayout(this);items.setOrientation(1);root.addView(items);render();
    }
    private void startExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "nebula-message-archive.json");
        startActivityForResult(intent, EXPORT_REQUEST);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=EXPORT_REQUEST || resultCode!=Activity.RESULT_OK || data==null)return;
        Uri uri=data.getData();
        if(uri==null)return;
        try(OutputStream stream=getContentResolver().openOutputStream(uri)) {
            if(stream==null)throw new java.io.IOException("No output stream");
            stream.write(NebulaMessageArchive.exportJson(this).getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this,"Архів експортовано",Toast.LENGTH_SHORT).show();
        }catch(Exception e){
            Toast.makeText(this,"Не вдалося експортувати архів",Toast.LENGTH_LONG).show();
        }
    }
    private int retentionChoice() {
        int days=NebulaMessageArchive.retentionDays(this);
        return days==7?0:days==30?1:days==90?2:3;
    }
    private int entryChoice() {
        int limit=NebulaMessageArchive.entryLimit(this);
        return limit==250?0:limit==1000?1:2;
    }
    /** Rules are applied only to locally stored copies, not to Telegram network messages. */
    private java.util.List<NebulaFilterEngine.Rule> cachedRules = java.util.Collections.emptyList();
    private boolean matchesArchiveRules(NebulaMessageArchive.Entry entry) {
        if(entry.versions.isEmpty())return false;
        java.util.List<NebulaFilterEngine.Rule> rules = cachedRules;
        if(rules.isEmpty())return false;
        int first=matchAnyVersion?0:entry.versions.size()-1;
        for(int i=first;i<entry.versions.size();i++) {
            String version=entry.versions.get(i);
            for(NebulaFilterEngine.Rule rule:rules) {
                if(NebulaFilterEngine.matches(rule,version,entry.chatKey))return true;
            }
        }
        return false;
    }
    private void render() {
        if(items==null)return;items.removeAllViews();
        NebulaMessageArchive.prune(this);
        List<NebulaMessageArchive.Entry> entries=NebulaMessageArchive.load(this);
        cachedRules = NebulaFilterEngine.load(this);
        NebulaToolUi.text(this,items,"Архівування: " + (NebulaMessageArchive.isPaused(this) ? "ПАУЗА" : NebulaMessageArchive.isEnabled(this) ? "УВІМКНЕНО" : "ВИМКНЕНО") +
            " · Збережено: "+entries.size()+" / "+NebulaMessageArchive.entryLimit(this)+
            " · термін: "+(NebulaMessageArchive.retentionDays(this)==0?"без обмеження":NebulaMessageArchive.retentionDays(this)+" днів"),13,0xffb8abc9);
        NebulaMessageArchive.Stats stats=NebulaMessageArchive.stats(this);
        NebulaToolUi.text(this,items,"Видалені: "+stats.deleted+" · Редаговані: "+stats.edited+
            " · Версії: "+stats.versions+" · Чати: "+stats.chats,13,0xffb8abc9);
        NebulaToolUi.text(this,items,"Архівування: "+(NebulaMessageArchive.isPaused(this)?"ПРИЗУПИНЕНО":"дозволено, якщо функція ввімкнена")+
            " · акаунт: "+(accountFilter<0?"усі":String.valueOf(accountFilter)),13,0xffb8abc9);
        NebulaToolUi.text(this,items,"Правила: "+(matchingRulesOnly?"лише збіги":unmatchedRulesOnly?"без збігів":"всі")+
            " · версії: "+(matchAnyVersion?"всі":"остання"),13,0xffb8abc9);
        NebulaToolUi.text(this,items,"Фільтр: "+(deletedOnly?"лише видалені":editedOnly?"лише редаговані":"усі повідомлення"),13,0xffb8abc9);
        NebulaToolUi.text(this,items,"Період: "+(recentDays==0?"весь час":recentDays+" дн.")+
            " · порядок: "+(newestFirst?"нові спочатку":"старі спочатку"),13,0xffb8abc9);
        String q=search.getText().toString().toLowerCase(Locale.ROOT);int count=0;
        long cutoff = recentDays==0 ? 0L : System.currentTimeMillis()-recentDays*86400000L;
        for(int step=0;step<entries.size();step++) {
            int i=newestFirst?entries.size()-1-step:step;
            NebulaMessageArchive.Entry e=entries.get(i);
            if (cutoff>0 && e.capturedAt<cutoff) continue;
            if(chatFilter != null && !chatFilter.equals(e.chatKey))continue;
            if(accountFilter>=0 && NebulaMessageArchive.accountOf(e)!=accountFilter)continue;
            if(deletedOnly && !e.deleted)continue;
            if(editedOnly && e.versions.size()<2)continue;
            boolean matched = matchesArchiveRules(e);
            if(matchingRulesOnly && !matched)continue;
            if(unmatchedRulesOnly && matched)continue;
            String last=e.versions.isEmpty()?"":e.versions.get(e.versions.size()-1);
            boolean found=e.chatKey.toLowerCase(Locale.ROOT).contains(q) ||
                String.valueOf(e.messageId).contains(q);
            if(!found)for(String version:e.versions)if(version.toLowerCase(Locale.ROOT).contains(q)){found=true;break;}
            if(!found)continue;
            count++;
            String heading=e.chatKey+" · #"+e.messageId+(e.deleted?" · видалено":"")+(matched?" · збіг правила":"");
            String date=DateFormat.getDateTimeInstance().format(new Date(e.capturedAt));
            NebulaToolUi.text(this,items,heading+"\n"+date+"\n"+last,15,0xffffffff);
            if(!last.isEmpty()) {
                final String copyText=last;
                NebulaToolUi.button(this,items,"Копіювати останню версію",()->
                    NebulaPrivacyPolicy.confirmClipboard(this,copyText,()->{
                        ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE))
                            .setPrimaryClip(android.content.ClipData.newPlainText("Nebula archive",copyText));
                        Toast.makeText(this,"Текст скопійовано",Toast.LENGTH_SHORT).show();
                    }));
            }
            if(e.versions.size()>1)NebulaToolUi.button(this,items,"Історія змін ("+e.versions.size()+")",()->{
                StringBuilder s=new StringBuilder();for(int j=0;j<e.versions.size();j++)s.append(j+1).append(". ").append(e.versions.get(j)).append("\n\n");
                new AlertDialog.Builder(this).setTitle("Збережені версії").setMessage(s.toString()).setPositiveButton("Закрити",null).show();
            });
            NebulaToolUi.button(this,items,"Видалити локальну копію #"+e.messageId,()->new AlertDialog.Builder(this)
                .setMessage("Видалити цю локальну копію?").setNegativeButton("Ні",null)
                .setPositiveButton("Так",(d,w)->{NebulaMessageArchive.delete(this,e.chatKey,e.messageId);render();}).show());
        }
        if(count==0)NebulaToolUi.text(this,items,"Збережених повідомлень немає.",15,0xffb8abc9);
        // Per-chat erasure is available when the search matches a complete chat key.
        if (!q.isEmpty() || chatFilter != null) {
            for (NebulaMessageArchive.Entry entry : entries) {
                if ((chatFilter != null && entry.chatKey.equals(chatFilter)) || entry.chatKey.toLowerCase(Locale.ROOT).equals(q)) {
                    final String selectedChat = entry.chatKey;
                    NebulaToolUi.button(this,items,"Очистити архів цього чату",()->
                        new AlertDialog.Builder(this).setTitle("Очистити архів чату?")
                            .setMessage("Локальні копії цього чату буде видалено.")
                            .setNegativeButton("Скасувати",null)
                            .setPositiveButton("Очистити",(d,w)->{
                                NebulaMessageArchive.clearChat(this,selectedChat);render();
                            }).show());
                    break;
                }
            }
        }
    }
}
