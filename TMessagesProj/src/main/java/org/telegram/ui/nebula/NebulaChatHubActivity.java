package org.telegram.ui.nebula;

import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Account-scoped, local chat toolbox. Never changes Telegram server state. */
public final class NebulaChatHubActivity extends NebulaProtectedActivity {
 private LinearLayout root;
 private String chatKey;
 private int account;
 private long dialogId;
 private static final int EXPORT_CHAT_REQUEST=9021;
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  if (!getIntent().hasExtra("nebula_dialog_id")) { finish(); return; }
  account=getIntent().getIntExtra("nebula_account",0);
  dialogId=getIntent().getLongExtra("nebula_dialog_id",0L);
  chatKey="telegram:"+account+":"+dialogId;
  root=NebulaToolUi.page(this,"Nebula · Центр чату","Локальні інструменти для цього чату. Дані не надсилаються в Telegram.");
  NebulaToolUi.text(this,root,"Акаунт: "+account+" · ID: "+dialogId,14,0xffc6a4ff);
  addGesturePad();
  NebulaToolUi.button(this,root,"Копіювати ID чату",()->copy(String.valueOf(dialogId)));
  NebulaToolUi.button(this,root,"Копіювати ключ чату",()->copy(chatKey));
  NebulaToolUi.button(this,root,"Швидко редагувати нотатку",()->editNoteInline());
  NebulaToolUi.button(this,root,"Редагувати нотатку",()->open(NebulaChatNotesActivity.class));
  NebulaToolUi.button(this,root,"Закладки цього чату",()->open(NebulaBookmarksActivity.class));
  NebulaToolUi.button(this,root,"Виключити / включити архівування чату",()->toggleArchiveExclusion());
  NebulaToolUi.button(this,root,"Увімкнути / вимкнути захист скриншотів чату",()->toggleSecureScreen());
  NebulaToolUi.button(this,root,"Профілі приватності цього чату",()->open(NebulaChatPrivacyPresetsActivity.class));
  NebulaToolUi.button(this,root,"Позначити / зняти приховування чату",()->toggleHidden());
  NebulaToolUi.button(this,root,"Увімкнути / вимкнути PIN для чату",()->togglePinLock());
  NebulaToolUi.button(this,root,"Індикатор друкування: перемкнути для чату",()->toggleGhostRule("suppress_typing", "індикатор друкування"));
  NebulaToolUi.button(this,root,"Підтвердження прочитання: перемкнути для чату",()->toggleGhostRule("suppress_chat_read", "підтвердження прочитання"));
  NebulaToolUi.button(this,root,"Видалити архів повідомлень цього чату",()->clearChatArchive());
  NebulaToolUi.button(this,root,"Статистика архіву цього чату",()->showArchiveStats());
  NebulaToolUi.button(this,root,"Повна локальна статистика цього чату",()->open(NebulaStatsActivity.class));
  NebulaToolUi.button(this,root,"Менеджер прихованих чатів",()->open(NebulaHiddenChatsActivity.class));
  NebulaToolUi.button(this,root,"Експортувати локальні дані чату (JSON)",()->exportChat());
  NebulaToolUi.button(this,root,"Видалити всі локальні чернетки чату",()->deleteAllDrafts());
  NebulaToolUi.button(this,root,"Редагувати правила",()->open(NebulaChatRulesActivity.class));
  NebulaToolUi.button(this,root,"Фільтри повідомлень цього чату",()->open(NebulaFilterActivity.class));
  NebulaToolUi.button(this,root,"Архів повідомлень цього чату",()->open(NebulaArchiveActivity.class));
  NebulaToolUi.button(this,root,"Створити локальну чернетку",()->open(NebulaDraftsActivity.class));
  NebulaToolUi.button(this,root,"Планувальник чернеток чату",()->open(NebulaDraftsActivity.class));
  NebulaToolUi.button(this,root,"Переглянути чернетки чату",()->open(NebulaDraftsActivity.class));
  NebulaToolUi.button(this,root,"Копіювати нотатку",()->copy(getSharedPreferences("nebula_chat_notes_v1",0).getString(chatKey,"")));
  NebulaToolUi.button(this,root,"Видалити нотатку",()->confirmDeleteNote());
  NebulaToolUi.button(this,root,"Скинути локальні правила чату",()->confirmDeleteRules());
  NebulaToolUi.button(this,root,"Показати локальний підсумок",()->showSummary());
  showSummaryInline();
 }
 private void addGesturePad() {
  android.widget.TextView pad=new android.widget.TextView(this);
  pad.setText("✦ Швидкі жести Nebula\nПодвійний дотик або свайп на цій панелі");
  pad.setTextColor(0xffe7d8ff);pad.setTextSize(15);pad.setGravity(android.view.Gravity.CENTER);
  pad.setBackgroundColor(0xff33234e);pad.setPadding(18,28,18,28);
  root.addView(pad,new LinearLayout.LayoutParams(-1,dpGesture(110)));
  android.view.GestureDetector detector=new android.view.GestureDetector(this,new android.view.GestureDetector.SimpleOnGestureListener(){
   @Override public boolean onDown(android.view.MotionEvent e){return true;}
   @Override public boolean onDoubleTap(android.view.MotionEvent e){return triggerGesture(NebulaGesturePolicy.DOUBLE_TAP);}
   @Override public boolean onFling(android.view.MotionEvent first,android.view.MotionEvent last,float vx,float vy){
    if(first==null||last==null)return false;
    float dx=last.getX()-first.getX(),dy=last.getY()-first.getY();
    if(Math.abs(dx)<dpGesture(65)||Math.abs(dx)<Math.abs(dy)*1.5f||Math.abs(vx)<250)return false;
    return triggerGesture(dx<0?NebulaGesturePolicy.SWIPE_LEFT:NebulaGesturePolicy.SWIPE_RIGHT);
   }
  });
  pad.setOnTouchListener((v,event)->{detector.onTouchEvent(event);return true;});
  NebulaToolUi.button(this,root,"Налаштувати глобальні жести",()->open(NebulaGesturesActivity.class));
  NebulaToolUi.button(this,root,"Налаштувати жести саме цього чату",()->chooseChatGesture());
 }
 private int dpGesture(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private boolean triggerGesture(String gesture){
  if(!NebulaGesturePolicy.enabled(this)){
   Toast.makeText(this,"Спочатку увімкніть жести в налаштуваннях",Toast.LENGTH_SHORT).show();return true;
  }
  String action=NebulaGesturePolicy.actionForChat(this,account,dialogId,gesture);
  switch(action){
   case NebulaGesturePolicy.NOTES:open(NebulaChatNotesActivity.class);break;
   case NebulaGesturePolicy.ARCHIVE:open(NebulaArchiveActivity.class);break;
   case NebulaGesturePolicy.DRAFTS:open(NebulaDraftsActivity.class);break;
   case NebulaGesturePolicy.STATS:open(NebulaStatsActivity.class);break;
   case NebulaGesturePolicy.RULES:open(NebulaChatRulesActivity.class);break;
   default:Toast.makeText(this,"Для цього жесту не призначено дію",Toast.LENGTH_SHORT).show();break;
  }
  return true;
 }
 private void chooseChatGesture() {
  String[] gestures={NebulaGesturePolicy.DOUBLE_TAP,NebulaGesturePolicy.SWIPE_LEFT,NebulaGesturePolicy.SWIPE_RIGHT};
  String[] labels={"Подвійний дотик","Свайп ліворуч (лише панель)","Свайп праворуч (лише панель)"};
  new AlertDialog.Builder(this).setTitle("Жести цього чату")
   .setItems(labels,(d,index)->chooseChatGestureAction(gestures[index],labels[index]))
   .setNegativeButton("Закрити",null).show();
 }
 private void chooseChatGestureAction(String gesture,String label) {
  String[] actions={NebulaGesturePolicy.NONE,NebulaGesturePolicy.NOTES,NebulaGesturePolicy.ARCHIVE,
   NebulaGesturePolicy.DRAFTS,NebulaGesturePolicy.STATS,NebulaGesturePolicy.RULES};
  String[] names={"Нічого","Нотатки","Архів","Чернетки","Статистика","Правила"};
  String[] options=new String[names.length+1];
  options[0]="Використовувати глобальну дію";
  System.arraycopy(names,0,options,1,names.length);
  String current=NebulaGesturePolicy.actionForChat(this,account,dialogId,gesture);
  int selected=0;
  if(NebulaGesturePolicy.hasChatOverride(this,account,dialogId,gesture)) {
   for(int i=0;i<actions.length;i++)if(actions[i].equals(current)){selected=i+1;break;}
  }
  new AlertDialog.Builder(this).setTitle(label).setSingleChoiceItems(options,selected,(dialog,which)->{
   boolean ok=which==0?NebulaGesturePolicy.clearChatAction(this,account,dialogId,gesture)
    :NebulaGesturePolicy.setChatAction(this,account,dialogId,gesture,actions[which-1]);
   Toast.makeText(this,ok?"Жест чату збережено":"Помилка збереження",Toast.LENGTH_SHORT).show();
   if(ok)dialog.dismiss();
  }).setNegativeButton("Скасувати",null).show();
 }
 private void editNoteInline() {
  SharedPreferences p=getSharedPreferences("nebula_chat_notes_v1",0);
  EditText input=new EditText(this);input.setMinLines(3);input.setText(p.getString(chatKey,""));
  new AlertDialog.Builder(this).setTitle("Нотатка чату").setView(input)
   .setNegativeButton("Скасувати",null).setPositiveButton("Зберегти",(d,w)->{
    String note=input.getText().toString();if(note.length()>20000)note=note.substring(0,20000);
    boolean ok=p.edit().putString(chatKey,note).commit();Toast.makeText(this,ok?"Нотатку збережено":"Помилка збереження",Toast.LENGTH_SHORT).show();
   }).show();
 }
 private JSONObject chatRules() {
  try{return new JSONObject(getSharedPreferences("nebula_chat_rules_v1",0).getString(chatKey,"{}"));}
  catch(JSONException e){return new JSONObject();}
 }
 private void toggleHidden() {
  boolean hidden=NebulaChatPolicy.enabled(this,account,dialogId,"hidden");
  new AlertDialog.Builder(this).setTitle("Локальна позначка прихованого чату")
   .setMessage("Nebula приховує позначені чати зі звичайного списку. Пошук, сповіщення та інші розділи можуть показувати цей чат. " +
     (hidden?"Зняти позначку?":"Додати позначку?"))
   .setNegativeButton("Скасувати",null)
   .setPositiveButton("Підтвердити",(d,w)->{
    if(NebulaChatPolicy.set(this,account,dialogId,"hidden",!hidden))
     Toast.makeText(this,"Локальну позначку оновлено",Toast.LENGTH_SHORT).show();
    else Toast.makeText(this,"Не вдалося зберегти",Toast.LENGTH_SHORT).show();
   }).show();
 }
 private void togglePinLock() {
  boolean active=NebulaChatPolicy.enabled(this,account,dialogId,"pin_lock");
  if (!active && !NebulaPin.enabled(this)) {
   new AlertDialog.Builder(this).setTitle("PIN ще не налаштовано")
    .setMessage("Спочатку створіть PIN Nebula. Без нього захист чату не можна ввімкнути.")
    .setNegativeButton("Скасувати",null)
    .setPositiveButton("Налаштувати PIN",(d,w)->open(NebulaPinActivity.class)).show();
   return;
  }
  new AlertDialog.Builder(this).setTitle("PIN-захист чату")
   .setMessage(active?"Вимкнути PIN-захист цього чату?":"Запитувати PIN Nebula під час відкриття цього чату? Це локальний захист інтерфейсу, не шифрування Telegram.")
   .setNegativeButton("Скасувати",null)
   .setPositiveButton("Підтвердити",(d,w)->{
    boolean saved=NebulaChatPolicy.set(this,account,dialogId,"pin_lock",!active);
    if(saved) NebulaChatLock.relock(account,dialogId);
    Toast.makeText(this,saved?"PIN-захист оновлено":"Не вдалося зберегти",Toast.LENGTH_SHORT).show();
   }).show();
 }
 private void toggleGhostRule(String key, String label) {
  boolean active=NebulaChatPolicy.enabled(this,account,dialogId,key);
  new AlertDialog.Builder(this).setTitle("Експериментальна приватність: "+label)
   .setMessage("Це обмежує лише окремі дії з відкритого чату. Інші шляхи Telegram, сервер і пристрої можуть повідомляти про активність. Глобальний перемикач має пріоритет. " +
    (active?"Вимкнути правило цього чату?":"Увімкнути правило цього чату?"))
   .setNegativeButton("Скасувати",null)
   .setPositiveButton("Підтвердити",(d,w)->{
    boolean saved=NebulaChatPolicy.set(this,account,dialogId,key,!active);
    Toast.makeText(this,saved?"Правило збережено":"Не вдалося зберегти правило",Toast.LENGTH_SHORT).show();
   }).show();
 }
 private void showArchiveStats() {
  try {
   int total=0,deleted=0,edited=0;
   for(NebulaMessageArchive.Entry e:NebulaMessageArchive.load(this)) {
    if(!chatKey.equals(e.chatKey))continue;
    total++;if(e.deleted)deleted++;if(e.versions.size()>1)edited++;
   }
   new AlertDialog.Builder(this).setTitle("Архів цього чату")
    .setMessage("Повідомлень: "+total+"\nПозначених видаленими: "+deleted+"\nЗ кількома версіями: "+edited)
    .setPositiveButton("Готово",null).show();
  } catch(Exception e) {Toast.makeText(this,"Не вдалося прочитати зашифрований архів",Toast.LENGTH_LONG).show();}
 }
 private void clearChatArchive() {
  new AlertDialog.Builder(this).setTitle("Видалити архів цього чату?")
   .setMessage("Усі локальні копії повідомлень цього чату будуть видалені. Архіви інших чатів залишаться. Дія незворотна.")
   .setNegativeButton("Скасувати",null)
   .setPositiveButton("Видалити",(d,w)->{
    try {
     int removed=NebulaMessageArchive.clearChat(this,chatKey);
     Toast.makeText(this,"Видалено записів: "+removed,Toast.LENGTH_LONG).show();
    } catch(Exception e) {Toast.makeText(this,"Не вдалося очистити архів",Toast.LENGTH_LONG).show();}
   }).show();
 }
 private void toggleSecureScreen() {
  boolean active=NebulaChatPolicy.enabled(this,account,dialogId,"secure_screen");
  new AlertDialog.Builder(this).setTitle("Захист скриншотів")
   .setMessage(active?"Вимкнути захист скриншотів для цього чату?":"Увімкнути FLAG_SECURE для цього чату? Захист не забороняє фотографувати екран іншим пристроєм.")
   .setNegativeButton("Скасувати",null)
   .setPositiveButton("Підтвердити",(d,w)->{
    boolean saved=NebulaChatPolicy.set(this,account,dialogId,"secure_screen",!active);
    Toast.makeText(this,saved?"Застосовано. Поверніться до чату.":"Не вдалося зберегти",Toast.LENGTH_SHORT).show();
   }).show();
 }
 private void toggleArchiveExclusion() {
  JSONObject rules=chatRules();boolean excluded=rules.optBoolean("exclude",false);
  new AlertDialog.Builder(this).setTitle("Архівування цього чату")
   .setMessage(excluded?"Дозволити локальне архівування цього чату?":"Виключити цей чат із локального архівування? Уже збережені записи залишаться.")
   .setNegativeButton("Скасувати",null).setPositiveButton("Підтвердити",(d,w)->{
    try {rules.put("exclude",!excluded);boolean ok=getSharedPreferences("nebula_chat_rules_v1",0).edit().putString(chatKey,rules.toString()).commit();
     Toast.makeText(this,ok?"Правило збережено":"Помилка збереження",Toast.LENGTH_SHORT).show();}
    catch(JSONException e){Toast.makeText(this,"Помилка збереження",Toast.LENGTH_SHORT).show();}
   }).show();
 }
 private void deleteAllDrafts() {
  if(draftCount()==0){Toast.makeText(this,"Чернеток немає",Toast.LENGTH_SHORT).show();return;}
  new AlertDialog.Builder(this).setTitle("Видалити всі чернетки цього чату?")
   .setMessage("Чернетки інших чатів і акаунтів залишаться.")
   .setNegativeButton("Скасувати",null).setPositiveButton("Видалити",(d,w)->{
    JSONArray old=drafts(),next=new JSONArray();for(int i=0;i<old.length();i++){JSONObject o=old.optJSONObject(i);if(!belongs(o))next.put(old.opt(i));}
    boolean ok=getSharedPreferences("nebula_drafts_v1",0).edit().putString("items",next.toString()).commit();
    Toast.makeText(this,ok?"Чернетки цього чату видалено":"Помилка збереження",Toast.LENGTH_SHORT).show();
   }).show();
 }
 private JSONObject chatExport() throws JSONException {
  JSONObject result=new JSONObject();result.put("format","nebula_chat_local_v1");result.put("account",account);
  result.put("dialog_id",dialogId);result.put("note",getSharedPreferences("nebula_chat_notes_v1",0).getString(chatKey,""));
  result.put("rules",chatRules());JSONArray filtered=new JSONArray(),all=drafts();
  for(int i=0;i<all.length();i++){JSONObject o=all.optJSONObject(i);if(belongs(o))filtered.put(o);}
  result.put("drafts",filtered);return result;
 }
 private void exportChat() {
  new AlertDialog.Builder(this).setTitle("Експорт приватних даних")
   .setMessage("JSON міститиме нотатку, правила й чернетки цього чату у відкритому вигляді. Збережіть його лише в надійному місці.")
   .setNegativeButton("Скасувати",null).setPositiveButton("Вибрати файл",(d,w)->{
    Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/json");intent.putExtra(Intent.EXTRA_TITLE,"nebula-chat-"+account+"-"+dialogId+".json");
    startActivityForResult(intent,EXPORT_CHAT_REQUEST);
   }).show();
 }
 @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
  super.onActivityResult(requestCode,resultCode,data);
  if(requestCode!=EXPORT_CHAT_REQUEST||resultCode!=RESULT_OK||data==null||data.getData()==null)return;
  try(android.os.ParcelFileDescriptor descriptor=getContentResolver().openFileDescriptor(data.getData(),"w")){
   if(descriptor==null)throw new java.io.IOException("No destination");
   try(java.io.FileOutputStream output=new java.io.FileOutputStream(descriptor.getFileDescriptor())){
    output.write(chatExport().toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));output.flush();
   }
   Toast.makeText(this,"Локальні дані експортовано",Toast.LENGTH_SHORT).show();
  }catch(Exception e){Toast.makeText(this,"Не вдалося експортувати дані",Toast.LENGTH_LONG).show();}
 }
 private void copy(String value) {
  NebulaPrivacyPolicy.confirmClipboard(this, value, () -> {
   ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Nebula",value));
   Toast.makeText(this,"Скопійовано",Toast.LENGTH_SHORT).show();
  });
 }
 private void open(Class<?> target) {
  Intent i=new Intent(this,target);i.putExtra("nebula_account",account);i.putExtra("nebula_dialog_id",dialogId);startActivity(i);
 }
 private JSONArray drafts() {
  try {return new JSONArray(getSharedPreferences("nebula_drafts_v1",0).getString("items","[]"));}
  catch (JSONException e) {return new JSONArray();}
 }
 private boolean belongs(JSONObject o) {return o!=null && o.optLong("dialog_id",Long.MIN_VALUE)==dialogId && o.optInt("account",-1)==account;}
 private int draftCount() {int n=0;JSONArray a=drafts();for(int i=0;i<a.length();i++)if(belongs(a.optJSONObject(i)))n++;return n;}
 private void newDraft() {
  EditText input=new EditText(this);input.setHint("Текст чернетки");input.setMinLines(3);
  new AlertDialog.Builder(this).setTitle("Нова локальна чернетка").setView(input).setNegativeButton("Скасувати",null)
   .setPositiveButton("Зберегти",(d,w)->{String body=input.getText().toString();if(body.trim().isEmpty())return;
    JSONArray a=drafts();JSONObject o=new JSONObject();try {o.put("chat",chatKey);o.put("text",body);o.put("time",0);o.put("account",account);o.put("dialog_id",dialogId);a.put(o);
    getSharedPreferences("nebula_drafts_v1",0).edit().putString("items",a.toString()).apply();Toast.makeText(this,"Збережено локально",Toast.LENGTH_SHORT).show();}catch(JSONException ignored){}
   }).show();
 }
 private void showDrafts() {
  JSONArray a=drafts();ArrayList<String> texts=new ArrayList<>();ArrayList<Integer> indexes=new ArrayList<>();
  for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(belongs(o)){texts.add(o.optString("text"));indexes.add(i);}}
  if(texts.isEmpty()){Toast.makeText(this,"Чернеток немає",Toast.LENGTH_SHORT).show();return;}
  String[] labels=new String[texts.size()];for(int i=0;i<labels.length;i++)labels[i]=texts.get(i).length()>90?texts.get(i).substring(0,90)+"…":texts.get(i);
  new AlertDialog.Builder(this).setTitle("Чернетки цього чату").setItems(labels,(d,index)->
   new AlertDialog.Builder(this).setTitle("Чернетка").setMessage(texts.get(index))
    .setPositiveButton("Копіювати",(dd,w)->copy(texts.get(index)))
    .setNeutralButton("Видалити",(dd,w)->confirmDeleteDraft(indexes.get(index)))
    .setNegativeButton("Назад",null).show()).show();
 }
 private void confirmDeleteDraft(int index) {
  new AlertDialog.Builder(this).setMessage("Видалити цю локальну чернетку?").setNegativeButton("Ні",null)
   .setPositiveButton("Видалити",(d,w)->{JSONArray old=drafts(),next=new JSONArray();for(int j=0;j<old.length();j++)if(j!=index)next.put(old.opt(j));
    getSharedPreferences("nebula_drafts_v1",0).edit().putString("items",next.toString()).apply();}).show();
 }
 private void confirmDeleteNote() {
  new AlertDialog.Builder(this).setMessage("Видалити нотатку тільки цього чату?").setNegativeButton("Ні",null)
   .setPositiveButton("Видалити",(d,w)->getSharedPreferences("nebula_chat_notes_v1",0).edit().remove(chatKey).apply()).show();
 }
 private void confirmDeleteRules() {
  new AlertDialog.Builder(this).setMessage("Скинути тільки локальні правила цього чату?").setNegativeButton("Ні",null)
   .setPositiveButton("Скинути",(d,w)->getSharedPreferences("nebula_chat_rules_v1",0).edit().remove(chatKey).apply()).show();
 }
 private String summary() {
  String note=getSharedPreferences("nebula_chat_notes_v1",0).getString(chatKey,"");
  String rules=getSharedPreferences("nebula_chat_rules_v1",0).getString(chatKey,"");
  StringBuilder s=new StringBuilder("Нотатка: ").append(note.isEmpty()?"немає":"є");
  s.append("\nЧернетки: ").append(draftCount());
  s.append("\nPIN-захист: ").append(NebulaChatPolicy.enabled(this,account,dialogId,"pin_lock")?"увімкнено":"вимкнено");
  s.append("\nПрихований у списку: ").append(NebulaChatPolicy.enabled(this,account,dialogId,"hidden")?"так":"ні");
  if(!rules.isEmpty()) {try {JSONObject o=new JSONObject(rules);for(String key:new String[]{"hidden","exclude","muted","shadow","attachments"})if(o.optBoolean(key))s.append("\n• ").append(key);}catch(JSONException ignored){}}
  else s.append("\nПравила: немає");
  return s.toString();
 }
 private void showSummary() {new AlertDialog.Builder(this).setTitle("Локальні дані чату").setMessage(summary()).setPositiveButton("Готово",null).show();}
 private void showSummaryInline() {NebulaToolUi.text(this,root,"Дані цього чату можна переглянути через «Показати локальний підсумок».",13,0xffb8abc9);}
}
