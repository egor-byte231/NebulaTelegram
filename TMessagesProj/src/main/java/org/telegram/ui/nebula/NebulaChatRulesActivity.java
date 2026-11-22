package org.telegram.ui.nebula;
import android.app.*;import android.os.*;import android.content.*;import android.widget.*;import org.json.*;import java.util.*;
/** Local chat-label policies. Does not intercept Telegram network traffic. */
public final class NebulaChatRulesActivity extends NebulaProtectedActivity {
 private static final String PREF="nebula_chat_rules_v1";private String boundKey;private LinearLayout root,list;private EditText query;
 @Override public void onCreate(Bundle b){super.onCreate(b);root=NebulaToolUi.page(this,"Правила для чатів","Для реального чату відкрийте правила через його Центр чату. Довільні назви тут створюють лише локальні записи, які не впливають на Telegram.");if(getIntent().hasExtra("nebula_dialog_id")){boundKey="telegram:"+getIntent().getIntExtra("nebula_account",0)+":"+getIntent().getLongExtra("nebula_dialog_id",0L);NebulaToolUi.button(this,root,"Правила поточного чату",()->edit(boundKey));}query=NebulaToolUi.edit(this,root,"Пошук чату");query.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){render();}public void afterTextChanged(android.text.Editable e){}});NebulaToolUi.button(this,root,"+ Додати чат",()->edit(null));
 NebulaToolUi.button(this,root,"Експорт правил чатів у буфер",()->exportRules());
 NebulaToolUi.button(this,root,"Імпорт правил із буфера (з підтвердженням)",()->confirmImport());list=new LinearLayout(this);list.setOrientation(1);root.addView(list);render();}
 private void exportRules(){
 try {
  String json=NebulaRuleTransfer.exportJson(this);
  android.content.ClipboardManager cb=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
  if(cb==null)throw new IllegalStateException("Clipboard unavailable");
  cb.setPrimaryClip(ClipData.newPlainText("Nebula chat rules",json));
  Toast.makeText(this,"Правила скопійовано. Буфер обміну може читатися іншими застосунками.",Toast.LENGTH_LONG).show();
 }catch(Exception e){Toast.makeText(this,"Помилка експорту: "+e.getMessage(),Toast.LENGTH_LONG).show();}
 }
 private void confirmImport(){
 android.content.ClipboardManager cb=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
 if(cb==null||!cb.hasPrimaryClip()){Toast.makeText(this,"Буфер порожній",Toast.LENGTH_SHORT).show();return;}
 ClipData clip=cb.getPrimaryClip();
 if(clip==null||clip.getItemCount()==0)return;
 CharSequence value=clip.getItemAt(0).coerceToText(this);
 final String json=value==null?"":value.toString();
 if(json.length()>1000000){Toast.makeText(this,"Дані завеликі",Toast.LENGTH_LONG).show();return;}
 new AlertDialog.Builder(this).setTitle("Імпортувати правила чатів?")
  .setMessage("Імпорт замінить правила чатів із тими самими ID. Інші правила залишаться. PIN та секрети не імпортуються. Переконайтеся, що довіряєте джерелу.")
  .setNegativeButton("Скасувати",null)
  .setPositiveButton("Імпортувати",(d,w)->{
   try{int n=NebulaRuleTransfer.importJson(this,json);Toast.makeText(this,"Імпортовано: "+n,Toast.LENGTH_LONG).show();render();}
   catch(Exception e){Toast.makeText(this,"Імпорт скасовано: "+e.getMessage(),Toast.LENGTH_LONG).show();}
  }).show();
 }
 private void edit(String old){SharedPreferences pref=getSharedPreferences(PREF,0);JSONObject obj=new JSONObject();try{if(old!=null)obj=new JSONObject(pref.getString(old,"{}"));}catch(Exception ignored){}
 LinearLayout form=new LinearLayout(this);form.setOrientation(1);EditText name=new EditText(this);name.setHint("Ключ telegram:<акаунт>:<ID> або локальна назва");name.setText(old==null?"":old);if(boundKey!=null&&boundKey.equals(old))name.setEnabled(false);form.addView(name);
 String[] labels={"Позначити прихованим у Nebula (не в Telegram)","Виключити з архівування","Вимкнути локальні сповіщення","Тіньовий бан (локальна позначка)","Зберігати вкладення","Заборонити скриншоти в цьому чаті","Вимагати PIN Nebula для відкриття чату","Не надсилати індикатор друкування в цьому чаті (експериментально)","Експериментально: пропускати окремі підтвердження прочитання"};String[] keys={"hidden","exclude","muted","shadow","attachments","secure_screen","pin_lock","suppress_typing","suppress_chat_read"};CheckBox[] checks=new CheckBox[keys.length];for(int i=0;i<keys.length;i++){checks[i]=new CheckBox(this);checks[i].setText(labels[i]);checks[i].setChecked(obj.optBoolean(keys[i],false));form.addView(checks[i]);}
 new AlertDialog.Builder(this).setTitle("Правило чату").setView(form).setNegativeButton("Скасувати",null).setPositiveButton("Зберегти",(d,w)->{String k=name.getText().toString().trim();if(k.isEmpty()||k.length()>128){Toast.makeText(this,"Назва має містити 1–128 символів",Toast.LENGTH_LONG).show();return;}JSONObject out=new JSONObject();try{for(int i=0;i<keys.length;i++)out.put(keys[i],checks[i].isChecked());}catch(Exception ignored){}SharedPreferences.Editor e=pref.edit();if(old!=null&&!old.equals(k))e.remove(old);e.putString(k,out.toString());if(!e.commit()){Toast.makeText(this,"Не вдалося зберегти правило",Toast.LENGTH_LONG).show();return;}if(checks[6].isChecked()&&!NebulaChatLock.pinConfigured(this))Toast.makeText(this,"Спочатку створіть PIN у налаштуваннях Nebula: чат залишиться заблокованим",Toast.LENGTH_LONG).show();render();}).show();}
 private void render(){if(list==null)return;list.removeAllViews();SharedPreferences pref=getSharedPreferences(PREF,0);ArrayList<String> keys=new ArrayList<>(pref.getAll().keySet());Collections.sort(keys,String.CASE_INSENSITIVE_ORDER);String q=query.getText().toString().toLowerCase(Locale.ROOT);for(String key:keys){if(!key.toLowerCase(Locale.ROOT).contains(q))continue;JSONObject o;try{o=new JSONObject(pref.getString(key,"{}"));}catch(Exception e){o=new JSONObject();}StringBuilder flags=new StringBuilder();for(String s:new String[]{"hidden","exclude","muted","shadow","attachments","secure_screen","pin_lock","suppress_typing","suppress_chat_read"})if(o.optBoolean(s,false))flags.append(s).append(" · ");NebulaToolUi.text(this,list,key+"\n"+(flags.length()==0?"Немає активних правил":flags.toString()),15,0xffffffff);NebulaToolUi.button(this,list,"Редагувати: "+key,()->edit(key));NebulaToolUi.button(this,list,"Видалити: "+key,()->new AlertDialog.Builder(this).setMessage("Видалити локальне правило?").setNegativeButton("Ні",null).setPositiveButton("Так",(d,w)->{if(pref.edit().remove(key).commit())render();else Toast.makeText(this,"Не вдалося видалити правило",Toast.LENGTH_LONG).show();}).show());}}
}
