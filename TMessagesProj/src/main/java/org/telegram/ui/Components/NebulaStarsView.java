/* Nebula decorative UI. Original Telegram source remains GPL-licensed. */
package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Subtle, non-interactive star shimmer over the dialogs header. */
public final class NebulaStarsView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] x = {0.12f, 0.29f, 0.47f, 0.68f, 0.87f, 0.96f};
    private final float[] y = {0.22f, 0.76f, 0.38f, 0.82f, 0.24f, 0.62f};
    private long start;

    public NebulaStarsView(Context context) {
        super(context);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        // Keep hardware acceleration enabled; simple circles need no software layer.
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start = android.os.SystemClock.uptimeMillis();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) return;
        if (!org.telegram.ui.nebula.NebulaAppearancePolicy.stars(getContext())) return;
        boolean animated = org.telegram.ui.nebula.NebulaAppearancePolicy.animations(getContext());
        float t = animated ? (android.os.SystemClock.uptimeMillis() - start) / 1000f : 0f;
        float strength = org.telegram.ui.nebula.NebulaAppearancePolicy.intensity(getContext()) / 100f;
        boolean contrast = org.telegram.ui.nebula.NebulaAppearancePolicy.highContrast(getContext());
        for (int i = 0; i < x.length; i++) {
            float pulse = (float) (0.5 + 0.5 * Math.sin(t * 0.9 + i * 1.7));
            int alpha = Math.min(255, (int) ((contrast ? 100 : 35) + pulse * (contrast ? 125 : 85) * strength));
            paint.setColor(Color.argb(alpha, 182, 115, 255));
            float cx = x[i] * getWidth();
            float cy = y[i] * getHeight();
            float r = getResources().getDisplayMetrics().density * (0.9f + pulse * 0.65f);
            canvas.drawCircle(cx, cy, r, paint);
        }
        if (animated && isAttachedToWindow() && getVisibility() == VISIBLE) postInvalidateDelayed(48);
    }
}
