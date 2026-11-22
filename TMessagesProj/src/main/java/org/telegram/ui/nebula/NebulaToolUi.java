package org.telegram.ui.nebula;
import android.app.Activity;
import android.graphics.Color;
import android.widget.*;
/** Common dark utility layout; no Telegram internal access. */
final class NebulaToolUi {
 static LinearLayout page(Activity a, String title, String description) {
  ScrollView scroll=new ScrollView(a);scroll.setBackgroundColor(0xff100d1c);
  LinearLayout root=new LinearLayout(a);root.setOrientation(1);int pad=(int)(18*a.getResources().getDisplayMetrics().density);root.setPadding(pad,pad,pad,pad);scroll.addView(root);a.setContentView(scroll);
  text(a,root,title,23,Color.WHITE);text(a,root,description,13,0xffb8abc9);return root;
 }
 static TextView text(Activity a,LinearLayout root,String s,int size,int color){TextView v=new TextView(a);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,8,0,8);root.addView(v);return v;}
 static Button button(Activity a,LinearLayout root,String label,Runnable run){Button b=new Button(a);b.setText(label);b.setAllCaps(false);b.setOnClickListener(v->run.run());root.addView(b);return b;}
 static EditText edit(Activity a,LinearLayout root,String hint){EditText e=new EditText(a);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(0xffaa99bb);root.addView(e);return e;}
}
