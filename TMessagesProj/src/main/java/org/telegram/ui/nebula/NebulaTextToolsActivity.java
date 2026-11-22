package org.telegram.ui.nebula;
import android.os.Bundle;import android.content.*;import android.widget.*;import java.util.Locale;
/** Offline text transformations; does not send or read Telegram messages. */
public final class NebulaTextToolsActivity extends NebulaProtectedActivity {
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout root=NebulaToolUi.page(this,"Nebula · Інструменти тексту","Обробка тексту локально; результат можна скопіювати в чат.");
 EditText input=NebulaToolUi.edit(this,root,"Вставте текст");input.setMinLines(5);TextView output=NebulaToolUi.text(this,root,"Результат з’явиться тут",16,-1);
 NebulaToolUi.button(this,root,"Прибрати зайві пробіли",()->output.setText(input.getText().toString().trim().replaceAll("[\\t ]+"," ").replaceAll("\\n{3,}","\\n\\n")));
 NebulaToolUi.button(this,root,"ВЕЛИКІ ЛІТЕРИ",()->output.setText(input.getText().toString().toUpperCase(Locale.getDefault())));
 NebulaToolUi.button(this,root,"малі літери",()->output.setText(input.getText().toString().toLowerCase(Locale.getDefault())));
 NebulaToolUi.button(this,root,"Кількість символів і слів",()->{String s=input.getText().toString().trim();output.setText("Символів: "+s.length()+" · Слів: "+(s.isEmpty()?0:s.split("\\s+").length));});
 NebulaToolUi.button(this,root,"Копіювати результат",()->{ClipboardManager c=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);c.setPrimaryClip(ClipData.newPlainText("Nebula",output.getText()));Toast.makeText(this,"Скопійовано",0).show();});}
}
