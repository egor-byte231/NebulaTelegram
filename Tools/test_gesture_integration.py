#!/usr/bin/env python3
"""Source-level regression guard; NOT an Android instrumentation test."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
chat = (root / "TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java").read_text()
policy = (root / "TMessagesProj/src/main/java/org/telegram/ui/nebula/NebulaGesturePolicy.java").read_text()
overlay = (root / "TMessagesProj/src/main/java/org/telegram/ui/nebula/NebulaChatGestureOverlay.java").read_text()
start = chat.index("public boolean hasDoubleTap(View view, int position)")
end = chat.index("public void onDoubleTap(View view, int position, float x, float y)", start)
assert "NebulaGesturePolicy.actionForChat(getParentActivity(), currentAccount, dialog_id," in chat[start:end]
assert "NebulaGesturePolicy.action(getParentActivity()," not in chat[start:end]
assert "NebulaChatGestureOverlay.performMessageDoubleTap(" in chat[end:end+1300]
assert "actionForChat(activity, account, dialogId, NebulaGesturePolicy.DOUBLE_TAP)" in overlay
assert 'if (!prefs(c).contains(key)) return action(c,gesture);' in policy
assert 'return validAction(value)?value:NONE;' in policy
assert 'return prefs(c).edit().remove(chatKey(account,dialogId,gesture)).commit();' in policy
print("PASS: double-tap availability and dispatch both use per-chat effective action")
