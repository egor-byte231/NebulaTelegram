package org.telegram.ui.nebula;
import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.net.Uri;
import android.graphics.Color;
import android.widget.*;
import java.io.*;
import java.util.*;
/** Explicit user-selected file imports to app-private storage, not automatic Telegram attachment saving. */
public final class NebulaStorageActivity extends NebulaProtectedActivity {
 private static final int PICK=4102; private LinearLayout files;
 private File directory(){File f=new File(getFilesDir(),"nebula_attachments");if(!f.exists())f.mkdirs();return f;}
 private long limit(){return getSharedPreferences("nebula_storage_v1",0).getInt("limit_mb",100)*1024L*1024L;}
 private long used(){long n=0;File[] a=directory().listFiles();if(a!=null)for(File f:a)if(f.isFile())n+=f.length();return n;}
 @Override public void onCreate(Bundle b){super.onCreate(b);refresh();}
 private void refresh(){ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xff100d1c);LinearLayout root=new LinearLayout(this);root.setPadding(24,24,24,24);root.setOrientation(1);scroll.addView(root);setContentView(scroll);
 TextView heading=new TextView(this);heading.setText("Nebula · Локальні вкладення");heading.setTextColor(Color.WHITE);heading.setTextSize(23);root.addView(heading);
 TextView details=new TextView(this);details.setText("Імпорт лише за вибором користувача. Файли зберігаються у приватній папці застосунку. Не є менеджером завантажень Telegram.\nВикористано: "+(used()/1024)+" КБ / "+(limit()/1024/1024)+" МБ");details.setTextColor(0xffb8abc9);root.addView(details);
 Spinner limits=new Spinner(this);String[] options={"25 МБ","100 МБ","250 МБ","500 МБ"};ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options);limits.setAdapter(adapter);int[] values={25,100,250,500};int saved=getSharedPreferences("nebula_storage_v1",0).getInt("limit_mb",100);int index=1;for(int i=0;i<values.length;i++)if(values[i]==saved)index=i;limits.setSelection(index);root.addView(limits);
 Button apply=new Button(this);apply.setText("Зберегти ліміт");apply.setOnClickListener(v->{getSharedPreferences("nebula_storage_v1",0).edit().putInt("limit_mb",values[limits.getSelectedItemPosition()]).apply();refresh();});root.addView(apply);
 Button pick=new Button(this);pick.setText("Імпортувати файл");pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK);});root.addView(pick);
 files=new LinearLayout(this);files.setOrientation(1);root.addView(files);File[] arr=directory().listFiles();if(arr==null)return;Arrays.sort(arr,(a,b)->a.getName().compareToIgnoreCase(b.getName()));for(File file:arr){if(!file.isFile())continue;TextView label=new TextView(this);label.setText(file.getName()+" · "+file.length()/1024+" КБ");label.setTextColor(Color.WHITE);files.addView(label);Button delete=new Button(this);delete.setText("Видалити файл");delete.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Видалити "+file.getName()+"?").setNegativeButton("Ні",null).setPositiveButton("Так",(d,w)->{if(!file.delete())Toast.makeText(this,"Не вдалося видалити",0).show();refresh();}).show());files.addView(delete);}}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=PICK||result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();String name="attachment";try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())name=c.getString(0);}catch(Exception ignored){}name=name==null?"attachment":name.replaceAll("[^\\p{L}\\p{N}._ -]","_");if(name.isEmpty()||name.equals(".")||name.equals(".."))name="attachment";
 File out=new File(directory(),System.currentTimeMillis()+"_"+name);long remaining=limit()-used();if(remaining<=0){Toast.makeText(this,"Досягнуто ліміт",0).show();return;}boolean success=false;try(InputStream in=getContentResolver().openInputStream(uri);OutputStream stream=new FileOutputStream(out)){if(in==null)throw new IOException("Cannot open file");byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1){remaining-=n;if(remaining<0)throw new IOException("Ліміт пам'яті перевищено");stream.write(buf,0,n);}success=true;}catch(Exception e){Toast.makeText(this,"Помилка імпорту: "+e.getMessage(),1).show();}finally{if(!success)out.delete();}refresh();}
}
