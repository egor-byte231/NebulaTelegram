package org.telegram.ui.nebula;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import org.telegram.messenger.MessageObject;

/** Read-only evaluation of messages already loaded in a regular Telegram chat.
 * Never mutates messages, changes network state, or persists scanned text.
 */
public final class NebulaLiveFilter {
    private NebulaLiveFilter() {}
    public static final int MAX_SCANNED = 500;
    public static final int MAX_RESULTS = 40;
    public static final class Hit {
        public final int messageId;
        public final String snippet;
        public final int ruleCount;
        Hit(int id, String snippet, int count) {
            this.messageId=id; this.snippet=snippet; this.ruleCount=count;
        }
    }
    public static final class Result {
        public final ArrayList<Hit> hits = new ArrayList<>();
        public int scanned, matching, activeRules, incomingMatches, outgoingMatches, skipped, ignoredNonText;
        public final java.util.LinkedHashMap<String, Integer> ruleMatches = new java.util.LinkedHashMap<>();
        public boolean truncated;
    }
    public static Result scan(Context context, int account, long dialogId, List<MessageObject> loaded) {
        return scan(context, account, dialogId, loaded, false, false);
    }
    /** Optional inspection controls: only matching direction and local keyword. */
    public static Result scan(Context context, int account, long dialogId, List<MessageObject> loaded,
                              boolean onlyIncoming, boolean onlyOutgoing) {
        Result result = new Result();
        if (context == null || loaded == null) return result;
        String key = "telegram:" + account + ":" + dialogId;
        ArrayList<NebulaFilterEngine.Rule> rules = new ArrayList<>();
        for (NebulaFilterEngine.Rule rule : NebulaFilterEngine.forChat(context, key)) {
            if (rule.enabled && rules.size() < 100) rules.add(rule);
        }
        result.activeRules = rules.size();
        for (NebulaFilterEngine.Rule rule : rules) result.ruleMatches.put(rule.id, 0);
        if (rules.isEmpty()) return result;
        for (MessageObject message : loaded) {
            if (result.scanned >= MAX_SCANNED) { result.truncated = true; break; }
            if (message == null || message.messageOwner == null || message.messageOwner.action != null) continue;
            boolean outgoing = message.isOutOwner();
            if ((onlyIncoming && outgoing) || (onlyOutgoing && !outgoing)) { result.skipped++; continue; }
            CharSequence source = message.messageText;
            if (source == null || source.length() == 0) { result.ignoredNonText++; continue; }
            result.scanned++;
            String text = source.length() > 4096 ? source.subSequence(0,4096).toString() : source.toString();
            int matches = 0;
            for (NebulaFilterEngine.Rule rule : rules) {
                if (NebulaFilterEngine.matches(rule,text,key,outgoing ? 2 : 1)) {
                    matches++;
                    result.ruleMatches.put(rule.id, result.ruleMatches.get(rule.id) + 1);
                }
            }
            if (matches == 0) continue;
            result.matching++;
            if (outgoing) result.outgoingMatches++; else result.incomingMatches++;
            if (result.hits.size() < MAX_RESULTS) {
                String preview = text.replace('\n',' ').replace('\r',' ').trim();
                if (preview.length() > 110) preview=preview.substring(0,110)+"…";
                result.hits.add(new Hit(message.getId(), preview, matches));
            }
        }
        return result;
    }
}
