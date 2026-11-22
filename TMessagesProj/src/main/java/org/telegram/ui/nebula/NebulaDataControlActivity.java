package org.telegram.ui.nebula;
import android.app.*;import android.os.*;import android.content.*;import android.widget.*;import java.util.*;
/** Clear selected Nebula-only local datasets. Never deletes Telegram chat history. */
public final class NebulaDataControlActivity extends NebulaProtectedActivity {
 private static final String[] PREFS={"nebula_chat_notes_v1","nebula_chat_rules_v1","nebula_features_v1","nebula_privacy_v1","nebula_appearance_v1"};
 private static final String[] NAMES={"Нотатки для чатів","Локальні правила чатів","Перемикачі Nebula","Параметри приватності","Параметри оформлення"};
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout root=NebulaToolUi.page(this,"Контроль локальних даних","Видалення незворотне. Це НЕ видаляє повідомлення, чати або дані серверів Telegram.");for(int i=0;i<PREFS.length;i++){final int n=i;SharedPreferences p=getSharedPreferences(PREFS[i],0);NebulaToolUi.button(this,root,"Очистити: "+NAMES[i]+" ("+p.getAll().size()+")",()->new AlertDialog.Builder(this).setTitle("Підтвердити видалення").setMessage("Очистити «"+NAMES[n]+"»? Відновити без резервної копії не вийде.").setNegativeButton("Скасувати",null).setPositiveButton("Видалити",(d,w)->{getSharedPreferences(PREFS[n],0).edit().clear().apply();recreate();}).show());}
 NebulaToolUi.text(this,root,"PIN, шаблони, чернетки, вкладення та інші сховища тут навмисно не очищаються.",13,0xffb8abc9);}
}
