package org.telegram.ui.nebula;

import android.content.Context;
import android.content.SharedPreferences;

/** Opt-in visual effects. No Telegram network or account state is modified. */
public final class NebulaAppearancePolicy {
    private static final String PREFS = "nebula_appearance_v1";
    private NebulaAppearancePolicy() { }
    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    public static boolean stars(Context context) { return prefs(context).getBoolean("stars", false); }
    public static boolean violet(Context context) { return prefs(context).getBoolean("violet", false); }
    public static boolean reducedMotion(Context context) { return prefs(context).getBoolean("reduced_motion", false); }
    public static boolean animations(Context context) { return prefs(context).getBoolean("animations", false) && !reducedMotion(context); }
    public static boolean highContrast(Context context) { return prefs(context).getBoolean("high_contrast", false); }
    public static int intensity(Context context) {
        return Math.max(0, Math.min(100, prefs(context).getInt("intensity", 30)));
    }
    /** A single atomic preference transaction for each named preset. */
    public static boolean preset(Context context, String name) {
        SharedPreferences.Editor editor = prefs(context).edit();
        if ("off".equals(name)) {
            editor.putBoolean("stars", false).putBoolean("animations", false)
                  .putBoolean("violet", false).putBoolean("reduced_motion", false)
                  .putBoolean("high_contrast", false);
        } else if ("nebula".equals(name)) {
            editor.putBoolean("stars", true).putBoolean("animations", true)
                  .putBoolean("violet", true).putBoolean("reduced_motion", false)
                  .putBoolean("high_contrast", false).putInt("intensity", 35);
        } else if ("accessible".equals(name)) {
            editor.putBoolean("stars", true).putBoolean("animations", false)
                  .putBoolean("violet", true).putBoolean("reduced_motion", true)
                  .putBoolean("high_contrast", true).putInt("intensity", 65);
        } else return false;
        return editor.commit();
    }
}
