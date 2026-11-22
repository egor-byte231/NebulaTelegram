package org.telegram.ui.nebula;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

/** Opt-in local reminders. Never sends messages, or exposes draft text in notifications. */
public final class NebulaDraftReminders extends BroadcastReceiver {
    private static final String PREF = "nebula_draft_reminders_v1";
    private static final String DRAFTS = "nebula_drafts_v1";
    private static final String ACTION = "org.telegram.nebula.DRAFT_DUE";
    private static final String CHANNEL = "nebula_drafts_due";
    private static final int REQUEST = 8173;
    private static final int NOTIFICATION = 8174;
    private static final long MIN_DELAY = 60000L;

    public static boolean enabled(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("enabled", false);
    }
    public static boolean setEnabled(Context context, boolean enabled) {
        if (!context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean("enabled", enabled).commit()) return false;
        reschedule(context);
        return true;
    }
    private static PendingIntent alarmIntent(Context context) {
        Intent intent = new Intent(context, NebulaDraftReminders.class).setAction(ACTION);
        return PendingIntent.getBroadcast(context, REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    public static void reschedule(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        PendingIntent alarm = alarmIntent(context);
        manager.cancel(alarm);
        if (!enabled(context)) return;
        long now = System.currentTimeMillis();
        long next = Long.MAX_VALUE;
        try {
            JSONArray drafts = new JSONArray(context.getSharedPreferences(DRAFTS, Context.MODE_PRIVATE).getString("items", "[]"));
            for (int i=0; i<drafts.length(); i++) {
                JSONObject item = drafts.optJSONObject(i);
                if (item == null || item.optBoolean("completed", false)) continue;
                long due = item.optLong("time", 0);
                if (due > now && due < next) next = due;
            }
        } catch (Exception ignored) { return; }
        if (next != Long.MAX_VALUE) manager.set(AlarmManager.RTC_WAKEUP, Math.max(next, now + MIN_DELAY), alarm);
    }
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            reschedule(context); return;
        }
        if (!ACTION.equals(action) || !enabled(context)) return;
        long now = System.currentTimeMillis();
        int dueCount = 0;
        try {
            JSONArray drafts = new JSONArray(context.getSharedPreferences(DRAFTS, Context.MODE_PRIVATE).getString("items", "[]"));
            for (int i=0; i<drafts.length(); i++) {
                JSONObject item = drafts.optJSONObject(i);
                if (item != null && !item.optBoolean("completed", false)) {
                    long due = item.optLong("time", 0);
                    if (due > 0 && due <= now) dueCount++;
                }
            }
        } catch (Exception ignored) { }
        if (dueCount > 0) notifyDue(context, dueCount);
        reschedule(context);
    }
    private static void notifyDue(Context context, int count) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Nebula · Нагадування чернеток", NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(context, NebulaDraftsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(context, REQUEST, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, CHANNEL) : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
               .setContentTitle("Nebula · Час перевірити чернетки")
               .setContentText("Запланованих чернеток: " + count)
               .setContentIntent(content).setAutoCancel(true);
        manager.notify(NOTIFICATION, builder.build());
    }
}
