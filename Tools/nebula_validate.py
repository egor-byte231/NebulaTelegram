"""Lightweight static checks before attempting the Telegram Android build."""
from pathlib import Path
import sys
root = Path(__file__).resolve().parents[1]
required = [
    'TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java',
    'TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ThemeColors.java',
    'TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java',
    'TMessagesProj/src/main/java/org/telegram/ui/Components/NebulaStarsView.java',
    'TMessagesProj/src/main/java/org/telegram/ui/nebula/NebulaFeaturesActivity.java',
    'TMessagesProj/src/main/java/org/telegram/ui/nebula/NebulaNotesActivity.java',
]
missing = [name for name in required if not (root / name).is_file()]
for name in required:
    print(('MISSING' if name in missing else 'OK') + ' ' + name)
if missing:
    sys.exit('Missing files: apply the Nebula v3 overlay to the full Telegram source before building')
print('Overlay presence check passed. Merge with original Telegram source before compiling; NOT a runtime test.')
