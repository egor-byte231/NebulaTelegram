package org.telegram.ui.nebula;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Opt-in gesture surface opened from the actual Telegram chat menu.
 * Does not intercept Telegram message swipes or alter messages.
 */
public final class NebulaChatGestureOverlay {
    private NebulaChatGestureOverlay() {}

    public static void show(Activity activity, int account, long dialogId) {
        if (activity == null || activity.isFinishing()) return;
        if (!NebulaGesturePolicy.enabled(activity)) {
            Toast.makeText(activity, "Увімкніть жести в налаштуваннях Nebula", Toast.LENGTH_LONG).show();
            return;
        }
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        int margin = dp(activity, 18);
        panel.setPadding(margin, margin, margin, margin);
        TextView pad = new TextView(activity);
        pad.setText("✦ NEBULA\n\nПодвійний дотик або свайп ліворуч / праворуч");
        pad.setTextSize(17);
        pad.setTextColor(0xffeee2ff);
        pad.setGravity(Gravity.CENTER);
        pad.setBackgroundColor(0xff302044);
        panel.addView(pad, new LinearLayout.LayoutParams(-1, dp(activity, 190)));
        TextView guide = new TextView(activity);
        guide.setText("Подвійний дотик: " + label(NebulaGesturePolicy.actionForChat(activity, account, dialogId, NebulaGesturePolicy.DOUBLE_TAP))
                + "\nЛіворуч: " + label(NebulaGesturePolicy.actionForChat(activity, account, dialogId, NebulaGesturePolicy.SWIPE_LEFT))
                + "\nПраворуч: " + label(NebulaGesturePolicy.actionForChat(activity, account, dialogId, NebulaGesturePolicy.SWIPE_RIGHT)));
        guide.setPadding(0, dp(activity, 14), 0, 0);
        panel.addView(guide);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Nebula · Жести чату")
                .setView(panel)
                .setNeutralButton("Налаштувати", (d, w) -> launch(activity, NebulaGesturesActivity.class, account, dialogId))
                .setNegativeButton("Закрити", null).create();
        GestureDetector detector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onDoubleTap(MotionEvent e) {
                return perform(activity, dialog, account, dialogId, NebulaGesturePolicy.DOUBLE_TAP);
            }
            @Override public boolean onFling(MotionEvent first, MotionEvent last, float vx, float vy) {
                if (first == null || last == null) return false;
                float dx = last.getX() - first.getX();
                float dy = last.getY() - first.getY();
                if (Math.abs(dx) < dp(activity, 60) || Math.abs(dx) < Math.abs(dy) * 1.5f || Math.abs(vx) < 250) return false;
                return perform(activity, dialog, account, dialogId, dx < 0 ? NebulaGesturePolicy.SWIPE_LEFT : NebulaGesturePolicy.SWIPE_RIGHT);
            }
        });
        pad.setOnTouchListener((view, event) -> { detector.onTouchEvent(event); return true; });
        dialog.show();
    }

    /** Invoked only by an explicit opt-in double tap on a normal Telegram message cell. */
    public static boolean performMessageDoubleTap(Activity activity, int account, long dialogId) {
        if (activity == null || activity.isFinishing() || !NebulaGesturePolicy.enabled(activity)) return false;
        String action = NebulaGesturePolicy.actionForChat(activity, account, dialogId, NebulaGesturePolicy.DOUBLE_TAP);
        if (NebulaGesturePolicy.NONE.equals(action)) return false;
        Class<?> target = targetFor(action);
        if (target == null) return false;
        launch(activity, target, account, dialogId);
        return true;
    }

    /** Opt-in left swipe on a normal Telegram message. Returns false to preserve native reply. */
    public static boolean performMessageSwipe(Activity activity, int account, long dialogId, String gesture) {
        if (activity == null || activity.isFinishing() || !NebulaGesturePolicy.enabled(activity)) return false;
        if (!NebulaGesturePolicy.SWIPE_LEFT.equals(gesture)) return false;
        String action = NebulaGesturePolicy.actionForChat(activity, account, dialogId, gesture);
        if (NebulaGesturePolicy.NONE.equals(action)) return false;
        Class<?> target = targetFor(action);
        if (target == null) return false;
        launch(activity, target, account, dialogId);
        return true;
    }

    private static Class<?> targetFor(String action) {
        switch (action) {
            case NebulaGesturePolicy.NOTES: return NebulaChatNotesActivity.class;
            case NebulaGesturePolicy.ARCHIVE: return NebulaArchiveActivity.class;
            case NebulaGesturePolicy.DRAFTS: return NebulaDraftsActivity.class;
            case NebulaGesturePolicy.STATS: return NebulaStatsActivity.class;
            case NebulaGesturePolicy.RULES: return NebulaChatRulesActivity.class;
            default: return null;
        }
    }

    private static boolean perform(Activity activity, AlertDialog dialog, int account, long dialogId, String gesture) {
        if (!NebulaGesturePolicy.enabled(activity)) { dialog.dismiss(); return true; }
        String action = NebulaGesturePolicy.actionForChat(activity, account, dialogId, gesture);
        Class<?> target;
        switch (action) {
            case NebulaGesturePolicy.NOTES: target = NebulaChatNotesActivity.class; break;
            case NebulaGesturePolicy.ARCHIVE: target = NebulaArchiveActivity.class; break;
            case NebulaGesturePolicy.DRAFTS: target = NebulaDraftsActivity.class; break;
            case NebulaGesturePolicy.STATS: target = NebulaStatsActivity.class; break;
            case NebulaGesturePolicy.RULES: target = NebulaChatRulesActivity.class; break;
            default:
                Toast.makeText(activity, "Для жесту не призначено дію", Toast.LENGTH_SHORT).show();
                return true;
        }
        dialog.dismiss();
        launch(activity, target, account, dialogId);
        return true;
    }

    private static void launch(Activity activity, Class<?> target, int account, long dialogId) {
        Intent intent = new Intent(activity, target);
        intent.putExtra("nebula_account", account);
        intent.putExtra("nebula_dialog_id", dialogId);
        activity.startActivity(intent);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static String label(String action) {
        switch (action) {
            case NebulaGesturePolicy.NOTES: return "Нотатки";
            case NebulaGesturePolicy.ARCHIVE: return "Архів";
            case NebulaGesturePolicy.DRAFTS: return "Чернетки";
            case NebulaGesturePolicy.STATS: return "Статистика";
            case NebulaGesturePolicy.RULES: return "Правила";
            default: return "Не призначено";
        }
    }
}
