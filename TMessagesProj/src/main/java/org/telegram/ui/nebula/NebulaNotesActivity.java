package org.telegram.ui.nebula;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** On-device scratchpad. Not synced to Telegram or any cloud service. */
public final class NebulaNotesActivity extends NebulaProtectedActivity {
    private static final String PREFS = "nebula_notes_local_v1";
    private static final String KEY = "scratchpad";
    private EditText editor;
    @Override protected void onPause() {
        super.onPause();
        if (editor != null) getSharedPreferences(PREFS,MODE_PRIVATE).edit()
            .putString(KEY,editor.getText().toString()).apply();
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff100d1c);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xff100d1c);
        int pad = (int)(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad,pad,pad,pad);
        TextView heading = new TextView(this);
        heading.setText("Nebula · Локальні нотатки");
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(22);
        layout.addView(heading);
        TextView help = new TextView(this);
        help.setText("Нотатки зберігаються тільки на цьому пристрої. Не використовуйте для паролів чи секретів.");
        help.setTextColor(0xffb8abc9);
        layout.addView(help);
        editor = new EditText(this);
        editor.setTextColor(Color.WHITE);
        editor.setHintTextColor(0xff9885ad);
        editor.setHint("Напишіть нотатку...");
        editor.setGravity(android.view.Gravity.TOP);
        editor.setMinLines(9);
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editor.setText(getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY,""));
        layout.addView(editor,new LinearLayout.LayoutParams(-1,0,1f));
        Button save = new Button(this);
        save.setText("Зберегти на пристрої");
        save.setOnClickListener(v -> {
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY,editor.getText().toString()).apply();
            Toast.makeText(this,"Нотатку збережено",Toast.LENGTH_SHORT).show();
        });
        layout.addView(save);
        setContentView(layout);
    }

}
