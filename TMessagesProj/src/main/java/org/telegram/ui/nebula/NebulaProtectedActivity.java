package org.telegram.ui.nebula;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

/** Prevents drawing Nebula utility data until its optional PIN has been checked. */
public abstract class NebulaProtectedActivity extends Activity {
    private boolean promptShowing;
    private boolean unlockedHere;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        // Private screen preference applies to Nebula screens, not Telegram chat windows.
        applySecureFlag();
    }
    private void applySecureFlag() {
        boolean secure = getSharedPreferences("nebula_features_v1", MODE_PRIVATE).getBoolean("privacy_2", false);
        if (secure || NebulaPin.enabled(this)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
    @Override protected void onResume() {
        super.onResume();
        applySecureFlag();
        if (!NebulaPin.enabled(this) || NebulaPin.isUnlocked()) {
            unlockedHere = true;
            if (getWindow().getDecorView() != null) getWindow().getDecorView().setVisibility(View.VISIBLE);
            return;
        }
        getWindow().getDecorView().setVisibility(View.INVISIBLE);
        showPin();
    }
    private void showPin() {
        if (promptShowing || isFinishing()) return;
        promptShowing = true;
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("PIN Nebula");
        input.setSingleLine(true);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Розблокувати Nebula")
            .setView(input)
            .setNegativeButton("Вийти", (d,w) -> finish())
            .setPositiveButton("Відкрити", null)
            .create();
        dialog.setCancelable(false);
        dialog.setOnDismissListener(d -> { promptShowing = false; if (!unlockedHere && !isFinishing()) finish(); });
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            long wait = NebulaPin.retryAfterMs(this);
            if (wait > 0) {
                Toast.makeText(this, "Зачекайте " + ((wait + 999) / 1000) + " с", Toast.LENGTH_LONG).show();
                return;
            }
            if (NebulaPin.verify(this, input.getText().toString())) {
                NebulaPin.unlock(); unlockedHere = true;
                getWindow().getDecorView().setVisibility(View.VISIBLE);
                dialog.dismiss();
            } else { input.setText(""); Toast.makeText(this, "Неправильний PIN", Toast.LENGTH_SHORT).show(); }
        }));
        dialog.show();
    }
    @Override protected void onStop() {
        super.onStop();
        unlockedHere = false;
        if (NebulaPrivacyPolicy.enabled(this, "lock_on_background")) NebulaPin.lock();
    }
}
