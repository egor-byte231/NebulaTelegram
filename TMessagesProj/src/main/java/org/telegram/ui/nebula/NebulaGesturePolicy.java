package org.telegram.ui.nebula;

import android.content.Context;
import android.content.SharedPreferences;

/** Local opt-in shortcuts; no Telegram server action is performed. */
public final class NebulaGesturePolicy {
 public static final String DOUBLE_TAP="double_tap", SWIPE_LEFT="swipe_left", SWIPE_RIGHT="swipe_right";
 public static final String NONE="none", NOTES="notes", ARCHIVE="archive", DRAFTS="drafts", STATS="stats", RULES="rules";
 private static final String PREF="nebula_gestures_v1";
 private NebulaGesturePolicy() {}
 private static SharedPreferences prefs(Context c) {return c.getApplicationContext().getSharedPreferences(PREF,Context.MODE_PRIVATE);}
 public static boolean enabled(Context c) {return prefs(c).getBoolean("enabled",false);}
 public static boolean validAction(String a) {return NONE.equals(a)||NOTES.equals(a)||ARCHIVE.equals(a)||DRAFTS.equals(a)||STATS.equals(a)||RULES.equals(a);}
 public static String action(Context c,String gesture) {
  if (!DOUBLE_TAP.equals(gesture)&&!SWIPE_LEFT.equals(gesture)&&!SWIPE_RIGHT.equals(gesture)) return NONE;
  String a=prefs(c).getString(gesture,NONE);return validAction(a)?a:NONE;
 }
 public static boolean setEnabled(Context c,boolean value) {return prefs(c).edit().putBoolean("enabled",value).commit();}
 public static boolean setAction(Context c,String gesture,String action) {
  if ((!DOUBLE_TAP.equals(gesture)&&!SWIPE_LEFT.equals(gesture)&&!SWIPE_RIGHT.equals(gesture))||!validAction(action)) return false;
  return prefs(c).edit().putString(gesture,action).commit();
 }
 /** A chat override is stored separately from global gestures and scoped to account + dialog. */
 private static String chatKey(int account,long dialogId,String gesture) {
  return "chat:"+account+":"+dialogId+":"+gesture;
 }
 public static String actionForChat(Context c,int account,long dialogId,String gesture) {
  if (!DOUBLE_TAP.equals(gesture)&&!SWIPE_LEFT.equals(gesture)&&!SWIPE_RIGHT.equals(gesture)) return NONE;
  String key=chatKey(account,dialogId,gesture);
  if (!prefs(c).contains(key)) return action(c,gesture);
  String value=prefs(c).getString(key,NONE);
  return validAction(value)?value:NONE;
 }
 public static boolean hasChatOverride(Context c,int account,long dialogId,String gesture) {
  return prefs(c).contains(chatKey(account,dialogId,gesture));
 }
 public static boolean setChatAction(Context c,int account,long dialogId,String gesture,String action) {
  if ((!DOUBLE_TAP.equals(gesture)&&!SWIPE_LEFT.equals(gesture)&&!SWIPE_RIGHT.equals(gesture))||!validAction(action)) return false;
  return prefs(c).edit().putString(chatKey(account,dialogId,gesture),action).commit();
 }
 public static boolean clearChatAction(Context c,int account,long dialogId,String gesture) {
  if (!DOUBLE_TAP.equals(gesture)&&!SWIPE_LEFT.equals(gesture)&&!SWIPE_RIGHT.equals(gesture)) return false;
  return prefs(c).edit().remove(chatKey(account,dialogId,gesture)).commit();
 }
 /** Only remove the three gesture overrides for one account and dialog, not global or other chats. */
 public static boolean clearChatOverrides(Context c,int account,long dialogId) {
  if (account < 0 || dialogId == 0) return false;
  return prefs(c).edit()
   .remove(chatKey(account,dialogId,DOUBLE_TAP))
   .remove(chatKey(account,dialogId,SWIPE_LEFT))
   .remove(chatKey(account,dialogId,SWIPE_RIGHT)).commit();
 }
 /** Explicit chat override summary, useful for auditing inherited vs locally assigned actions. */
 public static int chatOverrideCount(Context c,int account,long dialogId) {
  if (account < 0 || dialogId == 0) return 0;
  int count=0;
  for (String gesture:new String[]{DOUBLE_TAP,SWIPE_LEFT,SWIPE_RIGHT}) {
   if (hasChatOverride(c,account,dialogId,gesture)) count++;
  }
  return count;
 }
 public static boolean reset(Context c) {return prefs(c).edit().clear().commit();}
}
