package org.telegram.ui.nebula;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import android.text.InputType;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

/** On-device per-chat notes. IDs are local labels, not Telegram API identifiers. */
public final class NebulaChatNotesActivity extends NebulaProtectedActivity {
    private static final String PREF = "nebula_chat_notes_v1";
    private LinearLayout list;
    private String boundKey;
    private EditText filter;
    private int dp(int x) { return (int)(x*getResources().getDisplayMetrics().density+.5f); }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        if (getIntent().hasExtra("nebula_dialog_id")) {
            int account = getIntent().getIntExtra("nebula_account", 0);
            long dialog = getIntent().getLongExtra("nebula_dialog_id", 0L);
            boundKey = "telegram:" + account + ":" + dialog;
        }
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(16),dp(16),dp(16));root.setBackgroundColor(0xff100d1c);
        TextView title=new TextView(this);title.setText("Nebula · Нотатки для чатів");title.setTextSize(22);title.setTextColor(Color.WHITE);root.addView(title);
        TextView info=new TextView(this);info.setText("Локальні нотатки за власною назвою чату. Не синхронізуються з Telegram.");info.setTextColor(0xffb8abc9);root.addView(info);
        filter=new EditText(this);filter.setHint("Пошук за назвою");filter.setSingleLine(true);root.addView(filter);
        filter.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){render();}public void afterTextChanged(android.text.Editable e){}});
        if (boundKey != null) {
            TextView bound = new TextView(this);
            bound.setText("Поточний чат: " + boundKey + " (локально, без синхронізації)");
            bound.setTextColor(0xffc6a4ff); root.addView(bound);
            Button current = new Button(this); current.setText("Відкрити нотатку поточного чату");
            current.setOnClickListener(v -> edit(boundKey)); root.addView(current);
        }
        Button add=new Button(this);add.setText("+ Додати нотатку для чату");add.setOnClickListener(v->edit(null));root.addView(add);
        ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);render();
    }
    private void edit(String key) {
        SharedPreferences p=getSharedPreferences(PREF,MODE_PRIVATE);
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(18),0,dp(18),0);
        EditText name=new EditText(this);name.setHint("Назва чату");name.setSingleLine(true);name.setText(key==null?"":key);if (boundKey != null && boundKey.equals(key)) name.setEnabled(false);form.addView(name);
        EditText note=new EditText(this);note.setHint("Нотатка");note.setMinLines(4);note.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);note.setText(key==null?"":p.getString(key,""));form.addView(note);
        new AlertDialog.Builder(this).setTitle("Локальна нотатка").setView(form).setNegativeButton("Скасувати",null).setPositiveButton("Зберегти",(d,w)->{
            String k=name.getText().toString().trim();if(k.isEmpty()){Toast.makeText(this,"Вкажіть назву",Toast.LENGTH_SHORT).show();return;}
            SharedPreferences.Editor e=p.edit();if(key!=null&&!key.equals(k))e.remove(key);e.putString(k,note.getText().toString()).apply();render();
        }).show();
    }
    private void render(){if(list==null)return;list.removeAllViews();SharedPreferences p=getSharedPreferences(PREF,MODE_PRIVATE);
        ArrayList<String> keys=new ArrayList<>(p.getAll().keySet());Collections.sort(keys,String.CASE_INSENSITIVE_ORDER);
        String q=filter.getText().toString().toLowerCase(java.util.Locale.ROOT);
        for(String k:keys){if(!k.toLowerCase(java.util.Locale.ROOT).contains(q))continue;
            TextView t=new TextView(this);t.setText(k+"\n"+p.getString(k,""));t.setTextColor(Color.WHITE);t.setPadding(dp(8),dp(12),dp(8),dp(12));list.addView(t);
            LinearLayout actions=new LinearLayout(this);Button edit=new Button(this);edit.setText("Редагувати");edit.setOnClickListener(v->edit(k));actions.addView(edit);
            Button delete=new Button(this);delete.setText("Видалити");delete.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Видалити нотатку для «"+k+"»?").setNegativeButton("Ні",null).setPositiveButton("Так",(d,w)->{p.edit().remove(k).apply();render();}).show());actions.addView(delete);list.addView(actions);
        }
    }
}
