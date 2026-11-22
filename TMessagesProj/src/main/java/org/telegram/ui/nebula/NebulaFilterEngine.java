package org.telegram.ui.nebula;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Local, opt-in text rule evaluator. Never changes Telegram network state. */
public final class NebulaFilterEngine {
    public static final String PREF = "nebula_filter_rules_v2";
    public static final String RULES = "rules";
    private NebulaFilterEngine() {}
    public static final class Rule {
        public String id, phrase, chatKey;
        public boolean regex, ignoreCase, enabled;
        /** 0 = both, 1 = incoming only, 2 = outgoing only. */
        public int direction;
        public Rule(String id, String phrase, String chatKey, boolean regex, boolean ignoreCase, boolean enabled) {
            this.id=id; this.phrase=phrase; this.chatKey=chatKey; this.regex=regex;
            this.ignoreCase=ignoreCase; this.enabled=enabled; this.direction=0;
        }
        JSONObject json() throws Exception { JSONObject j=new JSONObject(); j.put("id",id);j.put("phrase",phrase);j.put("chatKey",chatKey);j.put("regex",regex);j.put("ignoreCase",ignoreCase);j.put("enabled",enabled);j.put("direction",direction);return j; }
        static Rule from(JSONObject j) {Rule r=new Rule(j.optString("id"),j.optString("phrase"),j.optString("chatKey"),j.optBoolean("regex"),j.optBoolean("ignoreCase",true),j.optBoolean("enabled",true));r.direction=j.optInt("direction",0);return r;}
    }
    public static List<Rule> load(Context c) {
        ArrayList<Rule> out=new ArrayList<>();
        SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        try { JSONArray a=new JSONArray(p.getString(RULES,"[]"));
            for(int i=0;i<a.length();i++){JSONObject j=a.optJSONObject(i);if(j!=null)out.add(Rule.from(j));}
        } catch(Exception ignored) {} return out;
    }
    public static boolean save(Context c,List<Rule> rules) {
        JSONArray a=new JSONArray();
        try {if (rules.size() > MAX_RULES) return false; for(Rule r:rules){validate(r);a.put(r.json());}}
        catch(Exception e){return false;}
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(RULES,a.toString()).commit();
    }
    private static final int MAX_RULES = 500;
    private static final int MAX_MATCH_TEXT = 4096;
    /** Restrict patterns to a safe subset: no backrefs, lookarounds or nested quantified groups. */
    private static void validateRegex(String expression) {
        if (expression.contains("(?"))
            throw new IllegalArgumentException("Lookarounds and special groups are unsupported");
        for (int i = 0; i + 1 < expression.length(); i++) {
            if (expression.charAt(i) == '\\' && expression.charAt(i + 1) >= '1' && expression.charAt(i + 1) <= '9')
                throw new IllegalArgumentException("Backreferences are unsupported");
        }
        // Quantified groups can trigger catastrophic backtracking, e.g. (a+)+.
        if (expression.matches("(?s).*[)]\\s*[+*{].*"))
            throw new IllegalArgumentException("Quantified groups are unsupported");
        Pattern.compile(expression);
    }
    public static void validate(Rule r) throws PatternSyntaxException {
        if(r.direction<0||r.direction>2)throw new IllegalArgumentException("Invalid direction");
        if(r.chatKey!=null&&r.chatKey.length()>160)throw new IllegalArgumentException("Chat key too long");
        if(r.phrase==null||r.phrase.trim().isEmpty())throw new IllegalArgumentException("Empty rule");
        if(r.phrase.length()>512)throw new IllegalArgumentException("Rule too long");
        if(r.regex)validateRegex(r.phrase);
    }
    private static int flags(boolean ignoreCase){return ignoreCase?Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE:0;}
    public static boolean matches(Rule r,String text,String chatKey){
        return matches(r,text,chatKey,0);
    }
    /** Direction 0 means an unknown source (local text test); 1 incoming, 2 outgoing. */
    public static boolean matches(Rule r,String text,String chatKey,int direction){
        if (direction != 0 && r.direction != 0 && r.direction != direction) return false;
        if(!r.enabled||text==null||r.phrase==null||r.phrase.isEmpty())return false;
        if (text.length() > MAX_MATCH_TEXT) text = text.substring(0, MAX_MATCH_TEXT);
        if(r.chatKey!=null&&!r.chatKey.isEmpty()&&!r.chatKey.equals(chatKey))return false;
        try {validate(r);return Pattern.compile(r.regex?r.phrase:Pattern.quote(r.phrase),flags(r.ignoreCase)).matcher(text).find();}
        catch(RuntimeException ignored){return false;}
    }
    /** Export a versioned local backup. No data is uploaded. */
    public static String exportJson(Context c) throws Exception {
        JSONObject backup = new JSONObject();
        backup.put("format", "nebula-filters"); backup.put("version", 1);
        JSONArray items = new JSONArray();
        for (Rule r : load(c)) items.put(r.json());
        backup.put("rules", items);
        return backup.toString(2);
    }
    /** Validate all entries before replacing any saved data. */
    public static boolean importJson(Context c, String raw) throws Exception {
        if (raw == null || raw.length() > 1024 * 1024) throw new IllegalArgumentException("Backup too large");
        JSONObject backup = new JSONObject(raw);
        if (!"nebula-filters".equals(backup.optString("format")) || backup.optInt("version") != 1)
            throw new IllegalArgumentException("Unsupported backup format");
        JSONArray a = backup.getJSONArray("rules");
        if (a.length() > 500) throw new IllegalArgumentException("Too many rules");
        ArrayList<Rule> result = new ArrayList<>();
        java.util.HashSet<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject item = a.getJSONObject(i);
            Rule r = Rule.from(item); validate(r);
            if (r.id == null || r.id.isEmpty() || !ids.add(r.id)) throw new IllegalArgumentException("Duplicate or empty rule ID");
            if (r.chatKey != null && r.chatKey.length() > 160) throw new IllegalArgumentException("Chat key too long");
            result.add(r);
        }
        return save(c, result);
    }
    public static boolean setAllEnabled(Context c, boolean enabled) {
        List<Rule> rules = load(c);
        for (Rule r : rules) r.enabled = enabled;
        return save(c, rules);
    }
    public static List<Rule> forChat(Context c, String chatKey) {
        ArrayList<Rule> result = new ArrayList<>();
        for (Rule r : load(c)) if (r.chatKey == null || r.chatKey.isEmpty() || r.chatKey.equals(chatKey)) result.add(r);
        return result;
    }
    public static List<Rule> matching(Context c,String text,String chatKey){
        ArrayList<Rule> result=new ArrayList<>();for(Rule r:load(c))if(matches(r,text,chatKey))result.add(r);return result;
    }
}
