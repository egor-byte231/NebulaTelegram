package org.telegram.ui.nebula;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Local, account-scoped chat policies. No Telegram network-side effects. */
public final class NebulaChatPolicy {
    private NebulaChatPolicy() {}
    public static String key(int account, long dialogId) {
        return "telegram:" + account + ":" + dialogId;
    }
    public static JSONObject read(Context context, int account, long dialogId) {
        SharedPreferences prefs = context.getSharedPreferences("nebula_chat_rules_v1", Context.MODE_PRIVATE);
        try { return new JSONObject(prefs.getString(key(account, dialogId), "{}")); }
        catch (Exception ignored) { return new JSONObject(); }
    }
    public static boolean enabled(Context context, int account, long dialogId, String name) {
        return read(context, account, dialogId).optBoolean(name, false);
    }

    /** Atomically apply an opt-in privacy preset while preserving unrelated chat rules. */
    public static boolean applyPrivacyPreset(Context context, int account, long dialogId, String preset) {
        if (context == null || account < 0 || dialogId == 0L) return false;
        final boolean screen, typing, read, hidden;
        switch (preset) {
            case "standard": screen=false; typing=false; read=false; hidden=false; break;
            case "screen": screen=true; typing=false; read=false; hidden=false; break;
            case "quiet": screen=true; typing=true; read=true; hidden=false; break;
            case "hidden": screen=true; typing=true; read=true; hidden=true; break;
            default: return false;
        }
        JSONObject rules = read(context, account, dialogId);
        try {
            rules.put("secure_screen", screen);
            rules.put("suppress_typing", typing);
            rules.put("suppress_chat_read", read);
            rules.put("hidden", hidden);
        } catch (Exception e) { return false; }
        return context.getSharedPreferences("nebula_chat_rules_v1", Context.MODE_PRIVATE)
            .edit().putString(key(account, dialogId), rules.toString()).commit();
    }
    public static boolean set(Context context, int account, long dialogId, String name, boolean value) {
        if (!"secure_screen".equals(name) && !"exclude".equals(name) && !"pin_lock".equals(name) && !"hidden".equals(name) && !"suppress_typing".equals(name) && !"suppress_chat_read".equals(name)) return false;
        JSONObject policy = read(context, account, dialogId);
        try { policy.put(name, value); }
        catch (Exception e) { return false; }
        return context.getSharedPreferences("nebula_chat_rules_v1", Context.MODE_PRIVATE)
            .edit().putString(key(account, dialogId), policy.toString()).commit();
    }
}
