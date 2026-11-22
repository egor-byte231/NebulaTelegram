package org.telegram.ui.nebula;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.*;

/** Configure gestures used by the dedicated shortcut pad in each chat hub. */
public final class NebulaGesturesActivity extends NebulaProtectedActivity {
 private LinearLayout root;
 private static final String[] KEYS={NebulaGesturePolicy.DOUBLE_TAP,NebulaGesturePolicy.SWIPE_LEFT,NebulaGesturePolicy.SWIPE_RIGHT};
 private static final String[] LABELS={"Подвійний дотик","Свайп ліворуч","Свайп праворуч"};
 private static final String[] ACTIONS={NebulaGesturePolicy.NONE,NebulaGesturePolicy.NOTES,NebulaGesturePolicy.ARCHIVE,NebulaGesturePolicy.DRAFTS,NebulaGesturePolicy.STATS,NebulaGesturePolicy.RULES};
 private static final String[] NAMES={"Нічого","Нотатки чату","Архів чату","Чернетки чату","Статистика чату","Правила чату"};
 @Override public void onCreate(Bundle b){super.onCreate(b);render();}
 private void render(){
  root=NebulaToolUi.page(this,"Nebula · Жести","Подвійний дотик по звичайному повідомленню може відкривати обрану дію Nebula замість реакції Telegram. Свайп ліворуч по звичайному повідомленню може замінити стандартну відповідь Telegram. Свайп праворуч поки працює лише на спеціальній панелі. За замовчуванням усе вимкнено.");
  Switch toggle=new Switch(this);toggle.setText("Увімкнути жести Nebula");toggle.setTextColor(-1);toggle.setChecked(NebulaGesturePolicy.enabled(this));
  toggle.setOnCheckedChangeListener((v,checked)->{if(!NebulaGesturePolicy.setEnabled(this,checked)){Toast.makeText(this,"Не вдалося зберегти",Toast.LENGTH_SHORT).show();v.setChecked(!checked);}});root.addView(toggle);
  for(int i=0;i<KEYS.length;i++){final String key=KEYS[i], label=LABELS[i];NebulaToolUi.button(this,root,label+" → "+name(NebulaGesturePolicy.action(this,key)),()->choose(key,label));}
  // Chat-specific overrides must never be erased by a global reset without clear notice.
  final int account=getIntent().getIntExtra("account",-1);
  final long dialogId=getIntent().getLongExtra("dialog_id",0L);
  if(account>=0 && dialogId!=0L){
   int count=NebulaGesturePolicy.chatOverrideCount(this,account,dialogId);
   TextView summary=new TextView(this);
   summary.setText("Для цього чату: " + count + " індивідуальних налаштувань із 3. Значення «Нічого» теж є індивідуальним винятком.");
   summary.setTextColor(0xffe5d6ff);
   summary.setPadding(0,16,0,12);
   root.addView(summary);
   NebulaToolUi.button(this,root,"Скинути лише жести цього чату",()->new AlertDialog.Builder(this)
    .setTitle("Повернути глобальні жести для цього чату?")
    .setMessage("Буде видалено тільки три індивідуальні налаштування поточного чату. Інші чати й глобальні жести залишаться без змін.")
    .setNegativeButton("Скасувати",null)
    .setPositiveButton("Повернути",(d,w)->{
     if(NebulaGesturePolicy.clearChatOverrides(this,account,dialogId))render();
     else Toast.makeText(this,"Не вдалося зберегти",Toast.LENGTH_SHORT).show();
    }).show());
  }
  NebulaToolUi.button(this,root,"Скинути всі жести",()->new AlertDialog.Builder(this).setTitle("Скинути жести?").setNegativeButton("Скасувати",null).setPositiveButton("Скинути",(d,w)->{if(NebulaGesturePolicy.reset(this))render();else Toast.makeText(this,"Не вдалося скинути",Toast.LENGTH_SHORT).show();}).show());
 }
 private String name(String action){for(int i=0;i<ACTIONS.length;i++)if(ACTIONS[i].equals(action))return NAMES[i];return NAMES[0];}
 private void choose(String key,String label){
  int selected=0;String current=NebulaGesturePolicy.action(this,key);for(int i=0;i<ACTIONS.length;i++)if(ACTIONS[i].equals(current))selected=i;
  new AlertDialog.Builder(this).setTitle(label).setSingleChoiceItems(NAMES,selected,(dialog,which)->{if(NebulaGesturePolicy.setAction(this,key,ACTIONS[which])){dialog.dismiss();render();}else Toast.makeText(this,"Не вдалося зберегти",Toast.LENGTH_SHORT).show();}).setNegativeButton("Скасувати",null).show();
 }
}
