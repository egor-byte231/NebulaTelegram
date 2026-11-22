package org.telegram.ui.nebula;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Opt-in local draft planner. Never sends Telegram messages automatically. */
public final class NebulaDraftsActivity extends NebulaProtectedActivity {
 private static final String PREF="nebula_drafts_v1";
 private static final int MAX_DRAFTS=500, MAX_TEXT=20000;
 private LinearLayout list;
 private EditText message,target;
 private TextView status;
 private long scheduledAt;
 private int account=-1;
 private long dialogId=Long.MIN_VALUE;
 private boolean chatOnly=false, dueOnly=false;
 private EditText searchInput;
 private boolean scheduledOnly=false, completedOnly=false;
 private final Set<Integer> selectedDrafts=new HashSet<>();
 private TextView selectionStatus;
 private final SimpleDateFormat dateFormat=new SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault());
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  Intent intent=getIntent();
  chatOnly=intent.hasExtra("nebula_dialog_id") && intent.hasExtra("nebula_account");
  if(chatOnly){account=intent.getIntExtra("nebula_account",0);dialogId=intent.getLongExtra("nebula_dialog_id",0);}
  ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xff100d1c);
  LinearLayout root=new LinearLayout(this);root.setPadding(24,24,24,24);root.setOrientation(1);scroll.addView(root);setContentView(scroll);
  NebulaToolUi.text(this,root,chatOnly?"Nebula · Чернетки чату":"Nebula · Планувальник чернеток",23,0xffffffff);
  NebulaToolUi.text(this,root,"Локальні чернетки. За бажанням можна ввімкнути системні нагадування; автоматичного надсилання немає.",14,0xffb8abc9);
  Switch reminderSwitch=new Switch(this);reminderSwitch.setText("Нагадувати про заплановані чернетки (вимкнено за замовчуванням)");reminderSwitch.setTextColor(0xffffffff);reminderSwitch.setChecked(NebulaDraftReminders.enabled(this));root.addView(reminderSwitch);
  reminderSwitch.setOnCheckedChangeListener((v,checked)->{if(!NebulaDraftReminders.setEnabled(this,checked)){Toast.makeText(this,"Не вдалося зберегти",Toast.LENGTH_SHORT).show();v.setChecked(!checked);}else if(checked && android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},8175);}});
  NebulaToolUi.text(this,root,"Нагадування приблизні: Android може затримувати їх через економію батареї. Текст повідомлень у сповіщеннях не показується.",13,0xffb8abc9);
  target=new EditText(this);target.setHint("Назва чату (для себе)");target.setSingleLine(true);
  if(chatOnly){target.setText("Чат "+dialogId);target.setEnabled(false);}root.addView(target);
  message=new EditText(this);message.setHint("Текст чернетки");message.setMinLines(3);root.addView(message);
  Button date=new Button(this);date.setText("Вибрати дату та час");root.addView(date);
  date.setOnClickListener(v->pickDate(date));
  Button reset=new Button(this);reset.setText("Без запланованої дати");root.addView(reset);
  reset.setOnClickListener(v->{scheduledAt=0;date.setText("Вибрати дату та час");});
  Button save=new Button(this);save.setText("Зберегти чернетку");root.addView(save);
  save.setOnClickListener(v->saveDraft(date));
  Button filter=new Button(this);filter.setText("Показати тільки заплановані на зараз");root.addView(filter);
  filter.setOnClickListener(v->{dueOnly=!dueOnly;filter.setText(dueOnly?"Показати всі чернетки":"Показати тільки заплановані на зараз");render();});
  searchInput=new EditText(this);searchInput.setSingleLine(true);searchInput.setHint("Пошук у тексті та назві чату");root.addView(searchInput);
  searchInput.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){render();}
   public void afterTextChanged(android.text.Editable e){}
  });
  Button scheduled=new Button(this);scheduled.setText("Лише чернетки з датою");root.addView(scheduled);
  scheduled.setOnClickListener(v->{scheduledOnly=!scheduledOnly;scheduled.setText(scheduledOnly?"Показати також без дати":"Лише чернетки з датою");render();});
  Button completedFilter=new Button(this);completedFilter.setText("Показати виконані чернетки");root.addView(completedFilter);
  completedFilter.setOnClickListener(v->{completedOnly=!completedOnly;completedFilter.setText(completedOnly?"Показати невиконані чернетки":"Показати виконані чернетки");render();});
  NebulaToolUi.text(this,root,"Пакетні дії: позначте потрібні чернетки нижче. Вибір діє лише в поточному списку.",14,0xffb8abc9);
  selectionStatus=new TextView(this);selectionStatus.setTextColor(0xffc6a4ff);root.addView(selectionStatus);
  Button selectVisible=new Button(this);selectVisible.setText("Вибрати всі показані");root.addView(selectVisible);
  selectVisible.setOnClickListener(v->{selectAllVisible();render();});
  Button clearSelection=new Button(this);clearSelection.setText("Зняти вибір");root.addView(clearSelection);
  clearSelection.setOnClickListener(v->{selectedDrafts.clear();render();});
  Button markSelected=new Button(this);markSelected.setText("Позначити вибрані виконаними");root.addView(markSelected);
  markSelected.setOnClickListener(v->bulkCompletion(true));
  Button reopenSelected=new Button(this);reopenSelected.setText("Повернути вибрані до невиконаних");root.addView(reopenSelected);
  reopenSelected.setOnClickListener(v->bulkCompletion(false));
  Button postponeSelected=new Button(this);postponeSelected.setText("Перенести вибрані на 1 годину");root.addView(postponeSelected);
  postponeSelected.setOnClickListener(v->bulkPostpone(60));
  Button deleteSelected=new Button(this);deleteSelected.setText("Видалити вибрані (з підтвердженням)");root.addView(deleteSelected);
  deleteSelected.setOnClickListener(v->bulkDelete());
  Button copyDue=new Button(this);copyDue.setText("Копіювати всі чернетки, час яких настав");root.addView(copyDue);
  copyDue.setOnClickListener(v->copyDueDrafts());
  Button removeDue=new Button(this);removeDue.setText("Видалити прострочені чернетки (з підтвердженням)");root.addView(removeDue);
  removeDue.setOnClickListener(v->removeDueDrafts());
  Button export=new Button(this);export.setText("Експорт видимих чернеток у JSON");root.addView(export);
  export.setOnClickListener(v->exportVisible());
  Button importButton=new Button(this);importButton.setText("Імпорт чернеток із JSON");root.addView(importButton);
  importButton.setOnClickListener(v->importDrafts());
  status=new TextView(this);status.setTextColor(0xffc6a4ff);root.addView(status);
  list=new LinearLayout(this);list.setOrientation(1);root.addView(list);render();
 }
 private void pickDate(Button date){
  Calendar c=Calendar.getInstance();if(scheduledAt>0)c.setTimeInMillis(scheduledAt);
  new DatePickerDialog(this,(dp,y,m,d)->new TimePickerDialog(this,(tp,h,min)->{
   c.set(y,m,d,h,min,0);c.set(Calendar.MILLISECOND,0);
   scheduledAt=c.getTimeInMillis();date.setText(dateFormat.format(new Date(scheduledAt)));
  },c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true).show(),c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
 }
 private JSONArray load(){try{return new JSONArray(getSharedPreferences(PREF,0).getString("items","[]"));}catch(JSONException e){return new JSONArray();}}
 private boolean store(JSONArray a){boolean saved=getSharedPreferences(PREF,0).edit().putString("items",a.toString()).commit();if(saved)NebulaDraftReminders.reschedule(this);return saved;}
 private boolean belongs(JSONObject o){return o!=null && (!chatOnly || (o.optInt("account",-1)==account && o.optLong("dialog_id",Long.MIN_VALUE)==dialogId));}
 private void saveDraft(Button date){
  String text=message.getText().toString().trim();
  if(text.isEmpty()||text.length()>MAX_TEXT){Toast.makeText(this,"Текст: від 1 до 20000 символів",Toast.LENGTH_SHORT).show();return;}
  JSONArray a=load();if(a.length()>=MAX_DRAFTS){Toast.makeText(this,"Досягнуто ліміт 500 чернеток",Toast.LENGTH_LONG).show();return;}
  try{
   JSONObject o=new JSONObject();o.put("chat",target.getText().toString());o.put("text",text);o.put("time",scheduledAt);
   o.put("created_at",System.currentTimeMillis());o.put("completed",false);
   if(chatOnly){o.put("account",account);o.put("dialog_id",dialogId);}
   a.put(o);
   if(!store(a)){Toast.makeText(this,"Помилка збереження",Toast.LENGTH_LONG).show();return;}
   message.setText("");scheduledAt=0;date.setText("Вибрати дату та час");if(!chatOnly)target.setText("");render();
  }catch(JSONException e){Toast.makeText(this,"Помилка чернетки",Toast.LENGTH_LONG).show();}
 }
 private void render(){
  list.removeAllViews();JSONArray a=load();ArrayList<Integer> indexes=new ArrayList<>();int due=0;
  long now=System.currentTimeMillis();
  String query=searchInput==null?"":searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
  for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(!belongs(o))continue;
   long t=o.optLong("time",0);boolean completed=o.optBoolean("completed",false);
   if(t>0&&t<=now&&!completed)due++;
   if(completedOnly!=completed)continue;
   if(scheduledOnly&&t<=0)continue;
   if(dueOnly&&!(t>0&&t<=now&&!o.optBoolean("completed",false)))continue;
   if(!query.isEmpty() && !(o.optString("text","").toLowerCase(Locale.ROOT).contains(query)||o.optString("chat","").toLowerCase(Locale.ROOT).contains(query)))continue;
   indexes.add(i);
  }
  Collections.sort(indexes,(x,y)->{
   JSONObject left=a.optJSONObject(x),right=a.optJSONObject(y);
   long l=left==null?0:left.optLong("time",0),r=right==null?0:right.optLong("time",0);
   if(l==0)l=Long.MAX_VALUE;if(r==0)r=Long.MAX_VALUE;
   return Long.compare(l,r);
  });
  selectedDrafts.retainAll(new HashSet<>(indexes));
  updateSelectionStatus();
  int upcoming=0;long nearest=Long.MAX_VALUE;
  for(int i=0;i<a.length();i++){
   JSONObject item=a.optJSONObject(i);if(!belongs(item)||item.optBoolean("completed",false))continue;
   long at=item.optLong("time",0);if(at>now){upcoming++;nearest=Math.min(nearest,at);}
  }
  status.setText("Показано: "+indexes.size()+" · Час настав: "+due+" · Майбутніх: "+upcoming
    +(nearest==Long.MAX_VALUE?"":" · Найближча: "+dateFormat.format(new Date(nearest))));
  for(int index:indexes){
   JSONObject o=a.optJSONObject(index);if(o==null)continue;
   String body=o.optString("text","");long t=o.optLong("time",0);
   boolean done=o.optBoolean("completed",false);
   String when=t>0?dateFormat.format(new Date(t))+(t<=now&&!done?" · час настав":""):"Без дати";
   if(done)when+=" · виконано";
   CheckBox pick=new CheckBox(this);pick.setText("Вибрати чернетку");pick.setTextColor(0xffc6a4ff);
   pick.setChecked(selectedDrafts.contains(index));list.addView(pick);
   pick.setOnCheckedChangeListener((button,checked)->{if(checked)selectedDrafts.add(index);else selectedDrafts.remove(index);updateSelectionStatus();});
   TextView label=new TextView(this);label.setText("\n"+o.optString("chat","")+" · "+when+"\n"+body);
   label.setTextColor(0xffffffff);list.addView(label);
   Button toggleDone=new Button(this);toggleDone.setText(done?"Позначити невиконаною":"Позначити виконаною");list.addView(toggleDone);
   toggleDone.setOnClickListener(v->setCompleted(index,!done));
   Button duplicate=new Button(this);duplicate.setText("Дублювати чернетку");list.addView(duplicate);
   duplicate.setOnClickListener(v->duplicateDraft(index));
   Button copy=new Button(this);copy.setText("Копіювати");list.addView(copy);
   copy.setOnClickListener(v->NebulaPrivacyPolicy.confirmClipboard(this,body,()->{
    ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
    cm.setPrimaryClip(ClipData.newPlainText("Nebula draft",body));
    Toast.makeText(this,"Скопійовано",Toast.LENGTH_SHORT).show();
   }));
   Button share=new Button(this);share.setText("Передати текст у застосунок…");list.addView(share);
   share.setOnClickListener(v->shareDraft(body));
   Button edit=new Button(this);edit.setText("Редагувати текст");list.addView(edit);
   edit.setOnClickListener(v->editDraft(index,body));
   Button reschedule=new Button(this);reschedule.setText("Змінити дату");list.addView(reschedule);
   reschedule.setOnClickListener(v->changeDate(index,t));
   if(!done){
    Button snooze10=new Button(this);snooze10.setText("Перенести на 10 хвилин");list.addView(snooze10);
    snooze10.setOnClickListener(v->postponeDraft(index,10));
    Button snooze60=new Button(this);snooze60.setText("Перенести на 1 годину");list.addView(snooze60);
    snooze60.setOnClickListener(v->postponeDraft(index,60));
   }
   Button delete=new Button(this);delete.setText("Видалити");list.addView(delete);
   delete.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Видалити чернетку?")
    .setNegativeButton("Ні",null).setPositiveButton("Видалити",(d,w)->{
     JSONArray old=load(),next=new JSONArray();for(int j=0;j<old.length();j++)if(j!=index)next.put(old.opt(j));
     if(store(next))render();else Toast.makeText(this,"Помилка збереження",Toast.LENGTH_LONG).show();
    }).show());
  }
 }
 /** Selection uses the current filtered view and never crosses chat scope. */
 private void updateSelectionStatus(){
  if(selectionStatus!=null)selectionStatus.setText("Вибрано чернеток: "+selectedDrafts.size());
 }
 private void selectAllVisible(){
  JSONArray all=load();long now=System.currentTimeMillis();
  String q=searchInput==null?"":searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
  selectedDrafts.clear();
  for(int i=0;i<all.length();i++){
   JSONObject o=all.optJSONObject(i);if(!belongs(o))continue;
   long t=o.optLong("time",0);boolean completed=o.optBoolean("completed",false);
   if(completedOnly!=completed||(scheduledOnly&&t<=0)||(dueOnly&&!(t>0&&t<=now&&!completed)))continue;
   if(!q.isEmpty()&&!o.optString("text","").toLowerCase(Locale.ROOT).contains(q)
     &&!o.optString("chat","").toLowerCase(Locale.ROOT).contains(q))continue;
   selectedDrafts.add(i);
  }
 }
 private void bulkCompletion(boolean completed){
  if(selectedDrafts.isEmpty()){Toast.makeText(this,"Спочатку виберіть чернетки",Toast.LENGTH_SHORT).show();return;}
  final int count=selectedDrafts.size();
  new AlertDialog.Builder(this).setTitle(completed?"Позначити виконаними?":"Повернути до невиконаних?")
   .setMessage("Змінити статус "+count+" локальних чернеток? Повідомлення Telegram не надсилатимуться.")
   .setNegativeButton("Скасувати",null).setPositiveButton("Підтвердити",(d,w)->{
    JSONArray all=load();long now=System.currentTimeMillis();int changed=0;
    for(Integer index:new HashSet<>(selectedDrafts)){
     JSONObject o=all.optJSONObject(index);if(!belongs(o))continue;
     try{o.put("completed",completed);o.put("completed_at",completed?now:0);changed++;}
     catch(JSONException ignored){}
    }
    if(store(all)){selectedDrafts.clear();render();Toast.makeText(this,"Оновлено: "+changed,Toast.LENGTH_SHORT).show();}
    else Toast.makeText(this,"Помилка збереження",Toast.LENGTH_LONG).show();
   }).show();
 }
 /** Snoozing changes only a local schedule; it does not send messages. */
 private void postponeDraft(int index,int minutes){
  JSONArray all=load();JSONObject item=all.optJSONObject(index);
  if(!belongs(item)||item.optBoolean("completed",false))return;
  long now=System.currentTimeMillis();
  try{
   item.put("time",now+minutes*60000L);
   if(store(all)){selectedDrafts.clear();render();Toast.makeText(this,"Нагадування перенесено",Toast.LENGTH_SHORT).show();}
   else Toast.makeText(this,"Помилка збереження",Toast.LENGTH_LONG).show();
  }catch(JSONException e){Toast.makeText(this,"Помилка зміни дати",Toast.LENGTH_LONG).show();}
 }
 private void bulkPostpone(int minutes){
  if(selectedDrafts.isEmpty()){Toast.makeText(this,"Спочатку виберіть чернетки",Toast.LENGTH_SHORT).show();return;}
  final Set<Integer> selected=new HashSet<>(selectedDrafts);
  new AlertDialog.Builder(this).setTitle("Перенести вибрані чернетки?")
   .setMessage("Встановити час через "+minutes+" хвилин для невиконаних вибраних чернеток? Автоматичного надсилання немає.")
   .setNegativeButton("Скасувати",null).setPositiveButton("Перенести",(d,w)->{
    JSONArray all=load();long newTime=System.currentTimeMillis()+minutes*60000L;int changed=0;
    for(Integer index:selected){JSONObject item=all.optJSONObject(index);
     if(!belongs(item)||item.optBoolean("completed",false))continue;
     try{item.put("time",newTime);changed++;}catch(JSONException ignored){}
    }
    if(changed==0){Toast.makeText(this,"Немає невиконаних чернеток",Toast.LENGTH_SHORT).show();return;}
    if(store(all)){selectedDrafts.clear();render();Toast.makeText(this,"Перенесено: "+changed,Toast.LENGTH_SHORT).show();}
    else Toast.makeText(this,"Помилка збереження",Toast.LENGTH_LONG).show();
   }).show();
 }
 private void bulkDelete(){
  if(selectedDrafts.isEmpty()){Toast.makeText(this,"Спочатку виберіть чернетки",Toast.LENGTH_SHORT).show();return;}
  final Set<Integer> toDelete=new HashSet<>(selectedDrafts);
  new AlertDialog.Builder(this).setTitle("Видалити вибрані чернетки?")
   .setMessage("Буде видалено до "+toDelete.size()+" локальних записів. Цю дію не можна скасувати.")
   .setNegativeButton("Скасувати",null).setPositiveButton("Видалити",(d,w)->{
    JSONArray all=load(),remaining=new JSONArray();int removed=0;
    for(int i=0;i<all.length();i++){
     JSONObject o=all.optJSONObject(i);
     if(toDelete.contains(i)&&belongs(o)){removed++;continue;}
     remaining.put(all.opt(i));
    }
    if(store(remaining)){selectedDrafts.clear();render();Toast.makeText(this,"Видалено: "+removed,Toast.LENGTH_SHORT).show();}
    else Toast.makeText(this,"Помилка збереження",Toast.LENGTH_LONG).show();
   }).show();
 }
 /** Completion is a local workflow flag; it never sends or deletes Telegram messages. */
 private void setCompleted(int index,boolean completed){
  JSONArray a=load();JSONObject o=a.optJSONObject(index);if(!belongs(o))return;
  try{o.put("completed",completed);o.put("completed_at",completed?System.currentTimeMillis():0);
   if(store(a))render();else Toast.makeText(this,"Помилка збереження",Toast.LENGTH_LONG).show();
  }catch(JSONException e){Toast.makeText(this,"Помилка запису",Toast.LENGTH_LONG).show();}
 }
 private void duplicateDraft(int index){
  JSONArray a=load();JSONObject original=a.optJSONObject(index);if(!belongs(original))return;
  if(a.length()>=MAX_DRAFTS){Toast.makeText(this,"Ліміт 500 чернеток",Toast.LENGTH_LONG).show();return;}
  try{JSONObject copy=new JSONObject(original.toString());copy.put("created_at",System.currentTimeMillis());
   copy.put("completed",false);copy.put("completed_at",0);a.put(copy);
   if(store(a))render();else Toast.makeText(this,"Помилка збереження",Toast.LENGTH_LONG).show();
  }catch(JSONException e){Toast.makeText(this,"Помилка дублювання",Toast.LENGTH_LONG).show();}
 }
 private void editDraft(int index,String previous){
  EditText input=new EditText(this);input.setText(previous);input.setMinLines(3);
  new AlertDialog.Builder(this).setTitle("Редагувати чернетку").setView(input).setNegativeButton("Скасувати",null)
   .setPositiveButton("Зберегти",(d,w)->{
    String text=input.getText().toString().trim();if(text.isEmpty()||text.length()>MAX_TEXT){Toast.makeText(this,"Невірна довжина",Toast.LENGTH_LONG).show();return;}
    JSONArray a=load();JSONObject o=a.optJSONObject(index);if(!belongs(o))return;
    try{o.put("text",text);if(store(a))render();}catch(JSONException ignored){}
   }).show();
 }
 private void changeDate(int index,long oldTime){
  final Calendar c=Calendar.getInstance();if(oldTime>0)c.setTimeInMillis(oldTime);
  new AlertDialog.Builder(this).setTitle("Дата чернетки").setItems(new String[]{"Вибрати дату та час","Прибрати дату"},(dialog,choice)->{
   if(choice==1){setDraftTime(index,0);return;}
   new DatePickerDialog(this,(dp,y,m,d)->new TimePickerDialog(this,(tp,h,min)->{
    c.set(y,m,d,h,min,0);c.set(Calendar.MILLISECOND,0);setDraftTime(index,c.getTimeInMillis());
   },c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true).show(),c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
  }).show();
 }
 private void setDraftTime(int index,long time){
  JSONArray a=load();JSONObject o=a.optJSONObject(index);if(!belongs(o))return;
  try{o.put("time",time);if(store(a))render();}catch(JSONException ignored){}
 }
 /** Explicit user action only: no automatic sending and no Telegram API mutation. */
 private void shareDraft(String body){
  try{
   Intent send=new Intent(Intent.ACTION_SEND);
   send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,body);
   startActivity(Intent.createChooser(send,"Передати чернетку"));
  }catch(ActivityNotFoundException e){Toast.makeText(this,"Немає застосунку для передачі",Toast.LENGTH_LONG).show();}
 }
 /** Copies due drafts for the current account/chat scope; never sends them. */
 private void copyDueDrafts(){
  JSONArray a=load();long now=System.currentTimeMillis();StringBuilder text=new StringBuilder();int count=0;
  for(int i=0;i<a.length();i++){
   JSONObject o=a.optJSONObject(i);if(!belongs(o))continue;
   long time=o.optLong("time",0);if(time<=0||time>now||o.optBoolean("completed",false))continue;
   String body=o.optString("text","");if(body.isEmpty())continue;
   if(text.length()+body.length()+80>100000){Toast.makeText(this,"Досягнуто обмеження 100000 символів",Toast.LENGTH_LONG).show();break;}
   if(count>0)text.append("\n\n---\n\n");
   text.append(o.optString("chat","Чат")).append(" · ").append(dateFormat.format(new Date(time))).append("\n").append(body);
   count++;
  }
  if(count==0){Toast.makeText(this,"Немає чернеток, час яких настав",Toast.LENGTH_SHORT).show();return;}
  String output=text.toString();final int total=count;
  NebulaPrivacyPolicy.confirmClipboard(this,output,()->{
   ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
   cm.setPrimaryClip(ClipData.newPlainText("Nebula due drafts",output));
   Toast.makeText(this,"Скопійовано чернеток: "+total,Toast.LENGTH_SHORT).show();
  });
 }
 /** Removes only due drafts in the currently selected chat, if scoped. */
 private void removeDueDrafts(){
  JSONArray a=load();long now=System.currentTimeMillis();int count=0;
  for(int i=0;i<a.length();i++){
   JSONObject o=a.optJSONObject(i);if(!belongs(o))continue;
   long t=o.optLong("time",0);if(t>0&&t<=now&&!o.optBoolean("completed",false))count++;
  }
  if(count==0){Toast.makeText(this,"Немає прострочених чернеток",Toast.LENGTH_SHORT).show();return;}
  final int expected=count;
  new AlertDialog.Builder(this).setTitle("Видалити прострочені чернетки?")
   .setMessage("Буде видалено "+expected+" локальних чернеток. Повідомлення Telegram не зміняться.")
   .setNegativeButton("Скасувати",null).setPositiveButton("Видалити",(d,w)->{
    JSONArray old=load(),remaining=new JSONArray();long cutoff=System.currentTimeMillis();int removed=0;
    for(int i=0;i<old.length();i++){
     JSONObject o=old.optJSONObject(i);long t=o==null?0:o.optLong("time",0);
     if(belongs(o)&&t>0&&t<=cutoff&&!o.optBoolean("completed",false)){removed++;continue;}
     remaining.put(old.opt(i));
    }
    if(store(remaining)){render();Toast.makeText(this,"Видалено: "+removed,Toast.LENGTH_SHORT).show();}
    else Toast.makeText(this,"Помилка збереження: чернетки не видалені",Toast.LENGTH_LONG).show();
   }).show();
 }
 /** Export only records visible under the current chat/search/time filters. */
 private void exportVisible(){
  JSONArray selected=new JSONArray(),all=load();long now=System.currentTimeMillis();
  String q=searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
  for(int i=0;i<all.length();i++){
   JSONObject o=all.optJSONObject(i);if(!belongs(o))continue;
   long t=o.optLong("time",0);
   if(completedOnly!=o.optBoolean("completed",false))continue;
   if(scheduledOnly&&t<=0)continue;
   if(dueOnly&&!(t>0&&t<=now&&!o.optBoolean("completed",false)))continue;
   if(!q.isEmpty()&&!o.optString("text","").toLowerCase(Locale.ROOT).contains(q)&&!o.optString("chat","").toLowerCase(Locale.ROOT).contains(q))continue;
   selected.put(o);
  }
  try{
   JSONObject backup=new JSONObject();backup.put("format","nebula-drafts");backup.put("version",1);backup.put("items",selected);
   String data=backup.toString(2);
   NebulaPrivacyPolicy.confirmClipboard(this,data,()->{
    ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
    cm.setPrimaryClip(ClipData.newPlainText("Nebula drafts JSON",data));
    Toast.makeText(this,"Експортовано "+selected.length()+" чернеток",Toast.LENGTH_LONG).show();
   });
  }catch(JSONException e){Toast.makeText(this,"Помилка експорту",Toast.LENGTH_LONG).show();}
 }
 private void importDrafts(){
  EditText input=new EditText(this);input.setHint("Вставте JSON Nebula Drafts");input.setMinLines(4);
  new AlertDialog.Builder(this).setTitle("Імпорт локальних чернеток").setView(input)
   .setNegativeButton("Скасувати",null).setPositiveButton("Перевірити",(d,w)->{
    final JSONArray incoming;
    try{
     String raw=input.getText().toString();if(raw.length()>1024*1024)throw new IllegalArgumentException("Завеликий JSON");
     JSONObject backup=new JSONObject(raw);
     if(!"nebula-drafts".equals(backup.optString("format"))||backup.optInt("version")!=1)throw new IllegalArgumentException("Невідомий формат");
     incoming=backup.getJSONArray("items");
     JSONArray existing=load();if(incoming.length()>MAX_DRAFTS||incoming.length()+existing.length()>MAX_DRAFTS)throw new IllegalArgumentException("Перевищено ліміт 500 чернеток");
     for(int i=0;i<incoming.length();i++){
      JSONObject o=incoming.getJSONObject(i);
      String body=o.getString("text");if(body.trim().isEmpty()||body.length()>MAX_TEXT)throw new IllegalArgumentException("Некоректний текст у записі "+(i+1));
      if(o.optString("chat","").length()>512)throw new IllegalArgumentException("Завелика назва чату");
      if(o.optLong("time",0)<0)throw new IllegalArgumentException("Некоректна дата");
      if(o.has("completed")&&!(o.opt("completed") instanceof Boolean))throw new IllegalArgumentException("Некоректний статус чернетки");
      if(chatOnly&&(o.optInt("account",-1)!=account||o.optLong("dialog_id",Long.MIN_VALUE)!=dialogId))
       throw new IllegalArgumentException("У цьому екрані можна імпортувати лише чернетки поточного чату");
     }
    }catch(Exception e){Toast.makeText(this,"Імпорт: "+e.getMessage(),Toast.LENGTH_LONG).show();return;}
    new AlertDialog.Builder(this).setTitle("Підтвердження імпорту")
     .setMessage("Додати "+incoming.length()+" чернеток? Наявні записи залишаться. Імпорт може містити приватний текст.")
     .setNegativeButton("Скасувати",null).setPositiveButton("Додати",(dialog,which)->{
      JSONArray all=load();if(all.length()+incoming.length()>MAX_DRAFTS){Toast.makeText(this,"Ліміт чернеток",Toast.LENGTH_LONG).show();return;}
      for(int i=0;i<incoming.length();i++)all.put(incoming.opt(i));
      if(store(all)){render();Toast.makeText(this,"Чернетки імпортовано",Toast.LENGTH_SHORT).show();}
      else Toast.makeText(this,"Не вдалося зберегти",Toast.LENGTH_LONG).show();
     }).show();
   }).show();
 }

}
