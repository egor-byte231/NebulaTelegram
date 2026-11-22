package org.telegram.ui.nebula;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.Map;

/** Local-only, opt-in privacy presets. No claims about server-side invisibility. */
public final class NebulaGhostPresets {
    private NebulaGhostPresets() { }
    private static final String PREF = "nebula_privacy_v1";
    private static final String RULES = "nebula_chat_rules_v1";

    /** Both flags are committed in one transaction, so presets cannot partially apply. */
    public static boolean apply(Context context, boolean typing, boolean reads) {
        if (context == null) return false;
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putBoolean("suppress_typing", typing)
            .putBoolean("suppress_chat_read", reads).commit();
    }

    public static int[] countChatExceptions(Context context) {
        int typing = 0, reads = 0;
        if (context == null) return new int[]{0, 0};
        for (Map.Entry<String, ?> e : context.getSharedPreferences(RULES, Context.MODE_PRIVATE).getAll().entrySet()) {
            if (!e.getKey().startsWith("telegram:") || !(e.getValue() instanceof String)) continue;
            try {
                JSONObject p = new JSONObject((String) e.getValue());
                if (p.optBoolean("suppress_typing", false)) typing++;
                if (p.optBoolean("suppress_chat_read", false)) reads++;
            } catch (Exception ignored) { }
        }
        return new int[]{typing, reads};
    }

    /** Clear only the two experimental ghost flags, preserving PIN, hidden chats and other rules.
     * A single commit prevents half-cleared rule sets. Returns false on storage failure.
     */
    public static boolean clearChatExceptions(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(RULES, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!entry.getKey().startsWith("telegram:") || !(entry.getValue() instanceof String)) continue;
            try {
                JSONObject item = new JSONObject((String) entry.getValue());
                if (!item.has("suppress_typing") && !item.has("suppress_chat_read")) continue;
                item.remove("suppress_typing");
                item.remove("suppress_chat_read");
                if (item.length() == 0) edit.remove(entry.getKey());
                else edit.putString(entry.getKey(), item.toString());
            } catch (Exception ignored) { /* Do not overwrite corrupt or unknown rules. */ }
        }
        return edit.commit();
    }
}
