#!/usr/bin/env python3
"""Static source regression guard; not an Android runtime test."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
chat=(root/'TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java').read_text()
overlay=(root/'TMessagesProj/src/main/java/org/telegram/ui/nebula/NebulaChatGestureOverlay.java').read_text()
start=chat.index('boolean nebulaSwipeHandled = false;')
end=chat.index('showFieldPanelForReply(slidingView.getMessageObject());',start)
block=chat[start:end]
assert 'NebulaChatGestureOverlay.performMessageSwipe(' in block
assert 'NebulaGesturePolicy.SWIPE_LEFT' in block
assert '!isSecretChat()' in block and '!isInScheduleMode()' in block
assert 'if (!nebulaSwipeHandled)' in block
assert 'NebulaGesturePolicy.enabled(activity)' in overlay
assert 'actionForChat(activity, account, dialogId, gesture)' in overlay
assert 'if (NebulaGesturePolicy.NONE.equals(action)) return false;' in overlay
print('PASS: opt-in left message swipe dispatch with Telegram reply fallback')
