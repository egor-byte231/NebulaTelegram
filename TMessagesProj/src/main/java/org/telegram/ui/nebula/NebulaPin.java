package org.telegram.ui.nebula;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import android.util.Base64;

/** Local PIN gate for Nebula utility screens, separate from Telegram passcode. */
final class NebulaPin {
    private static final String PREF = "nebula_pin_v1";
    private static final int ITERATIONS = 120000;
    private static long unlockedUntil;
    private static final int MAX_FAILURES = 5;
    private NebulaPin() { }
    static boolean enabled(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).contains("hash"); }
    static boolean isUnlocked() { return SystemClock.elapsedRealtime() < unlockedUntil; }
    static void unlock() { unlockedUntil = SystemClock.elapsedRealtime() + 30000L; }
    static void lock() { unlockedUntil = 0; }
    static boolean valid(String pin) { return pin != null && pin.matches("[0-9]{4,12}"); }
    private static byte[] derive(String pin, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
    static long retryAfterMs(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return Math.max(0L, p.getLong("blocked_until", 0L) - System.currentTimeMillis());
    }
    static boolean verify(Context c, String pin) {
        if (!enabled(c) || pin == null || retryAfterMs(c) > 0) return false;
        SharedPreferences prefs = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        try {
            byte[] salt = Base64.decode(prefs.getString("salt", ""), Base64.NO_WRAP);
            byte[] expected = Base64.decode(prefs.getString("hash", ""), Base64.NO_WRAP);
            boolean valid = MessageDigest.isEqual(expected, derive(pin, salt));
            if (valid) prefs.edit().remove("failures").remove("blocked_until").commit();
            else registerFailure(prefs);
            return valid;
        } catch (Exception e) { registerFailure(prefs); return false; }
    }
    private static void registerFailure(SharedPreferences p) {
        int failures = Math.min(30, p.getInt("failures", 0) + 1);
        SharedPreferences.Editor edit = p.edit().putInt("failures", failures);
        if (failures >= MAX_FAILURES) {
            int exponent = Math.min(6, failures - MAX_FAILURES);
            long delay = Math.min(300000L, 30000L * (1L << exponent));
            edit.putLong("blocked_until", System.currentTimeMillis() + delay);
        }
        edit.commit();
    }
    static boolean set(Context c, String pin) {
        if (!valid(pin)) return false;
        try {
            byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
            byte[] hash = derive(pin, salt);
            boolean saved = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP)).commit();
            if (saved) unlock();
            return saved;
        } catch (Exception e) { return false; }
    }
    static void remove(Context c) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().commit();
        lock();
    }
}
