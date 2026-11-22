package org.telegram.ui.nebula;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import java.util.List;

/** Opt-in, local-only masking of matching plain-text chat rows.
 * Never deletes or alters Telegram messages or changes their read state.
 */
public final class NebulaAutoMask {
    private NebulaAutoMask() {}
    private static final String PREF = "nebula_auto_mask_v1";
    private static final String ENABLED = "enabled";
    public static boolean enabled(Context context) {
        return context != null && context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(ENABLED, false);
    }
    public static boolean setEnabled(Context context, boolean enabled) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, enabled).commit();
    }
    /** Per-chat opt-out. Global filtering remains enabled for other chats. */
    public static boolean isChatExcluded(Context context, int account, long dialogId) {
        if (context == null) return false;
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean("exclude:" + account + ":" + dialogId, false);
    }
    public static boolean setChatExcluded(Context context, int account, long dialogId, boolean excluded) {
        if (context == null) return false;
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putBoolean("exclude:" + account + ":" + dialogId, excluded).commit();
    }
    /** Local exclusions inventory. No Telegram API calls or server-side changes. */
    public static java.util.List<String> excludedChats(Context context) {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        if (context == null) return result;
        java.util.Map<String, ?> all = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getAll();
        for (java.util.Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getKey().startsWith("exclude:") && Boolean.TRUE.equals(entry.getValue()))
                result.add(entry.getKey().substring("exclude:".length()));
        }
        java.util.Collections.sort(result);
        return result;
    }
    /** Remove all local exceptions, leaving the global on/off preference unchanged. */
    public static boolean clearChatExclusions(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) if (key.startsWith("exclude:")) editor.remove(key);
        return editor.commit();
    }
    /** Only plain text. Media, service messages and secret chats must never be masked. */
    public static boolean shouldMask(Context context, int account, long dialogId, MessageObject message) {
        if (!enabled(context) || isChatExcluded(context, account, dialogId) || message == null || message.messageOwner == null || message.messageOwner.action != null)
            return false;
        if (message.contentType != 0 || (message.messageOwner.media != null && !(message.messageOwner.media instanceof TLRPC.TL_messageMediaEmpty)))
            return false;
        CharSequence source = message.messageText;
        if (source == null || source.length() == 0) return false;
        String key = "telegram:" + account + ":" + dialogId;
        String text = source.length() > 4096 ? source.subSequence(0, 4096).toString() : source.toString();
        List<NebulaFilterEngine.Rule> rules = NebulaFilterEngine.forChat(context, key);
        int direction = message.isOutOwner() ? 2 : 1;
        for (NebulaFilterEngine.Rule rule : rules) {
            if (NebulaFilterEngine.matches(rule, text, key, direction)) return true;
        }
        return false;
    }
}
