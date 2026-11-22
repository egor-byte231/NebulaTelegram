package org.telegram.ui.nebula;

import android.content.Context;
import android.content.SharedPreferences;

/** Opt-in composer typing suppression; no claims about read receipts or online status. */
public final class NebulaGhostPolicy {
    private static final String PREF = "nebula_privacy_v1";
    private NebulaGhostPolicy() { }

    /** A temporary pause expires automatically, including after process restarts.
     * The permanent pause switch remains independent of timed pauses.
     */
    public static boolean isPaused(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (prefs.getBoolean("ghost_paused", false)) return true;
        long until = prefs.getLong("ghost_pause_until", 0L);
        return until > System.currentTimeMillis();
    }

    public static long remainingPauseMillis(Context context) {
        if (context == null) return 0L;
        long until = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong("ghost_pause_until", 0L);
        return Math.max(0L, until - System.currentTimeMillis());
    }

    /** The global setting and per-chat setting are independent opt-ins. */
    public static boolean suppressTyping(Context context, int account, long dialogId) {
        if (context == null || dialogId == 0) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (isPaused(context)) return false;
        return prefs.getBoolean("suppress_typing", false)
            || NebulaChatPolicy.enabled(context, account, dialogId, "suppress_typing");
    }

    /** Experimental: suppresses selected ChatActivity read acknowledgements only.
     * Other controller, notification, device and server paths are not intercepted.
     * Secret chats are excluded at the ChatActivity call sites.
     */
    public static boolean suppressChatRead(Context context, int account, long dialogId) {
        if (context == null || dialogId == 0) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (isPaused(context)) return false;
        return prefs.getBoolean("suppress_chat_read", false)
            || NebulaChatPolicy.enabled(context, account, dialogId, "suppress_chat_read");
    }

    public static boolean globalTypingSuppression(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return !isPaused(context) && prefs.getBoolean("suppress_typing", false);
    }
}
