from pathlib import Path
root=Path(__file__).resolve().parents[1]
policy=(root/'TMessagesProj/src/main/java/org/telegram/ui/nebula/NebulaGesturePolicy.java').read_text()
ui=(root/'TMessagesProj/src/main/java/org/telegram/ui/nebula/NebulaGesturesActivity.java').read_text()
assert 'clearChatOverrides(Context c,int account,long dialogId)' in policy
for key in ('DOUBLE_TAP','SWIPE_LEFT','SWIPE_RIGHT'):
 assert '.remove(chatKey(account,dialogId,'+key+'))' in policy
assert 'chatOverrideCount' in ui and 'clearChatOverrides(this,account,dialogId)' in ui
assert 'setPositiveButton("Повернути"' in ui
print('Chat-scoped gesture reset source checks passed')
