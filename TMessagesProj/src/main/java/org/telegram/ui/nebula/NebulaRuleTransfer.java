package org.telegram.ui.nebula;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Validated, local-only backup of per-chat rules; never transfers PIN secrets. */
public final class NebulaRuleTransfer {
    private static final String PREF = "nebula_chat_rules_v1";
    private static final String[] FLAGS = {"hidden", "exclude", "muted", "shadow", "attachments",
        "secure_screen", "pin_lock", "suppress_typing", "suppress_chat_read"};
    private static final int MAX_RULES = 2000;
    private NebulaRuleTransfer() { }

    /** Strict Telegram dialog keys only; legacy arbitrary labels are not exported. */
    public static boolean validKey(String key) {
        if (key == null || !key.matches("telegram:[0-9]{1,2}:-?[0-9]{1,20}")) return false;
        String[] parts = key.split(":");
        try {
            int account = Integer.parseInt(parts[1]);
            long dialog = Long.parseLong(parts[2]);
            return account >= 0 && account <= 9 && dialog != 0;
        } catch (NumberFormatException e) { return false; }
    }

    private static JSONObject sanitize(JSONObject source) throws Exception {
        JSONObject clean = new JSONObject();
        for (String flag : FLAGS) {
            Object value = source.opt(flag);
            if (value != null && value != JSONObject.NULL && !(value instanceof Boolean))
                throw new IllegalArgumentException("Non-boolean flag: " + flag);
            clean.put(flag, source.optBoolean(flag, false));
        }
        return clean;
    }

    public static String exportJson(Context context) throws Exception {
        JSONArray rules = new JSONArray();
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!validKey(entry.getKey()) || !(entry.getValue() instanceof String)) continue;
            JSONObject rule = new JSONObject();
            rule.put("key", entry.getKey());
            rule.put("flags", sanitize(new JSONObject((String) entry.getValue())));
            rules.put(rule);
            if (rules.length() > MAX_RULES) throw new IllegalStateException("Too many rules");
        }
        JSONObject result = new JSONObject();
        result.put("format", "nebula-chat-rules");
        result.put("version", 1);
        result.put("rules", rules);
        return result.toString(2);
    }

    /** All-or-nothing validation before one atomic preferences commit. Existing entries are retained. */
    public static int importJson(Context context, String json) throws Exception {
        if (json == null || json.length() > 1000000) throw new IllegalArgumentException("Backup too large");
        JSONObject root = new JSONObject(json);
        if (!"nebula-chat-rules".equals(root.optString("format")) || root.optInt("version") != 1)
            throw new IllegalArgumentException("Unsupported backup format");
        JSONArray entries = root.getJSONArray("rules");
        if (entries.length() > MAX_RULES) throw new IllegalArgumentException("Too many rules");
        Set<String> unique = new HashSet<>();
        JSONObject[] validated = new JSONObject[entries.length()];
        String[] keys = new String[entries.length()];
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            String key = entry.getString("key");
            if (!validKey(key) || !unique.add(key)) throw new IllegalArgumentException("Invalid/duplicate chat key");
            keys[i] = key;
            validated[i] = sanitize(entry.getJSONObject("flags"));
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < keys.length; i++) editor.putString(keys[i], validated[i].toString());
        if (!editor.commit()) throw new IllegalStateException("Could not save backup");
        return keys.length;
    }
}
