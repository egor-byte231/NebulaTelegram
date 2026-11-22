package org.telegram.ui.nebula;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.TextUtils;

/** Local-only privacy controls. No claim of changing Telegram server read receipts or presence. */
public final class NebulaPrivacyPolicy {
    private static final String PREF = "nebula_privacy_v1";
    private NebulaPrivacyPolicy() { }
    public static boolean enabled(Context context, String key) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(key, false);
    }
    /** Called when the main Telegram Activity stops (including background transitions). */
    public static void lockWhenBackgrounded(Context context) {
        if (enabled(context, "lock_on_background")) NebulaPin.lock();
    }
    public static void confirmClipboard(Context context, String value, Runnable action) {
        if (TextUtils.isEmpty(value)) return;
        if (!enabled(context, "clear_clipboard_warning")) { action.run(); return; }
        new AlertDialog.Builder(context).setTitle("Копіювати приватні дані?")
            .setMessage("Інші застосунки можуть отримати доступ до буфера обміну. Не копіюйте конфіденційні дані на чужому пристрої.")
            .setNegativeButton("Скасувати", null)
            .setPositiveButton("Копіювати", (dialog, which) -> action.run()).show();
    }
    public static void confirmExport(Context context, Runnable action) {
        if (!enabled(context, "sensitive_export_warning")) { action.run(); return; }
        new AlertDialog.Builder(context).setTitle("Підтвердити експорт")
            .setMessage("Експортований JSON не шифрується. Він може містити особисті повідомлення та нотатки.")
            .setNegativeButton("Скасувати", null)
            .setPositiveButton("Продовжити", (dialog, which) -> action.run()).show();
    }
}
