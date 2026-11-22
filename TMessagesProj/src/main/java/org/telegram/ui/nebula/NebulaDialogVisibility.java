package org.telegram.ui.nebula;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import org.telegram.tgnet.TLRPC;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Local inbox presentation filter. Never edits Telegram's canonical dialog collection. */
public final class NebulaDialogVisibility {
    private NebulaDialogVisibility() {}

    public static ArrayList<TLRPC.Dialog> filter(Context context, int account,
                                                  ArrayList<TLRPC.Dialog> dialogs) {
        if (context == null || dialogs == null || dialogs.isEmpty()) return dialogs;
        SharedPreferences prefs = context.getSharedPreferences("nebula_chat_rules_v1", Context.MODE_PRIVATE);
        String prefix = "telegram:" + account + ":";
        Set<Long> hidden = new HashSet<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(prefix) || !(entry.getValue() instanceof String)) continue;
            try {
                long id = Long.parseLong(key.substring(prefix.length()));
                if (id != 0 && new JSONObject((String) entry.getValue()).optBoolean("hidden", false))
                    hidden.add(id);
            } catch (Exception ignored) { /* Malformed local policy must not hide chats. */ }
        }
        if (hidden.isEmpty()) return dialogs;
        ArrayList<TLRPC.Dialog> visible = new ArrayList<>(dialogs.size());
        for (TLRPC.Dialog dialog : dialogs) {
            if (dialog == null || !hidden.contains(dialog.id)) visible.add(dialog);
        }
        return visible;
    }
}
