package org.telegram.ui.nebula;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import java.util.HashSet;
import java.util.Set;

/** Optional per-chat local PIN gate. Uses the existing salted Nebula PIN verifier.
 * The Telegram passcode and server-side encryption are unaffected.
 * No chat is unlocked unless the PIN is configured and verified.
 */
public final class NebulaChatLock {
    private static final Set<String> authorized = new HashSet<>();
    private NebulaChatLock() {}
    public static boolean pinConfigured(Activity activity) { return NebulaPin.enabled(activity); }
    public static boolean needsUnlock(Activity activity, int account, long dialogId) {
        return NebulaChatPolicy.enabled(activity, account, dialogId, "pin_lock")
            && !authorized.contains(NebulaChatPolicy.key(account, dialogId));
    }
    /** Hide chat content before presenting the system-owned dialog. */
    public static void guard(Activity activity, View content, int account, long dialogId, Runnable onCancel) {
        if (content == null || !needsUnlock(activity, account, dialogId)) return;
        content.setVisibility(View.INVISIBLE);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("PIN Nebula");
        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle("Захищений чат Nebula")
            .setMessage(NebulaPin.enabled(activity) ? "Введіть PIN для перегляду цього чату" : "PIN Nebula ще не створено. Налаштуйте його в центрі приватності.")
            .setView(input)
            .setNegativeButton("Назад", (d, w) -> onCancel.run())
            .setPositiveButton("Розблокувати", null)
            .create();
        dialog.setCancelable(false);
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!NebulaPin.enabled(activity)) {
                Toast.makeText(activity, "Спочатку створіть PIN Nebula", Toast.LENGTH_LONG).show(); return;
            }
            long delay = NebulaPin.retryAfterMs(activity);
            if (delay > 0) {
                Toast.makeText(activity, "Спробуйте через " + ((delay + 999) / 1000) + " с", Toast.LENGTH_LONG).show(); return;
            }
            if (NebulaPin.verify(activity, input.getText().toString())) {
                authorized.add(NebulaChatPolicy.key(account, dialogId));
                content.setVisibility(View.VISIBLE);
                dialog.dismiss();
            } else {
                input.setText("");
                Toast.makeText(activity, "Неправильний PIN", Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
    }
    /** Re-lock a chat when it is no longer the foreground fragment. */
    public static void relock(int account, long dialogId) {
        authorized.remove(NebulaChatPolicy.key(account, dialogId));
    }
}
