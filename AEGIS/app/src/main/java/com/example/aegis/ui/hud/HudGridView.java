package com.example.aegis.ui.hud;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.example.aegis.R;

/** Fundo com grade holográfica e uma linha de varredura descendo. */
public class HudGridView extends View {

    private final Paint line = new Paint();
    private final Paint scan = new Paint();
    private final float step;
    private final float band;
    private final int accent;
    private float scanY;
    private ValueAnimator animator;

    public HudGridView(Context context) {
        this(context, null);
    }

    public HudGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        step = Hud.dp(context, 28);
        band = Hud.dp(context, 90);
        accent = ContextCompat.getColor(context, R.color.aegis_cyan);
        line.setColor(ColorUtils.setAlphaComponent(accent, 0x14));
        line.setStrokeWidth(Hud.dp(context, 1));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        scan.setShader(new LinearGradient(0, 0, 0, band,
                new int[]{ColorUtils.setAlphaComponent(accent, 0x00), ColorUtils.setAlphaComponent(accent, 0x26),
                        ColorUtils.setAlphaComponent(accent, 0x00)}, null, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(4500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            scanY = (float) a.getAnimatedValue() * (getHeight() + band) - band;
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        for (float x = 0; x < w; x += step) canvas.drawLine(x, 0, x, h, line);
        for (float y = 0; y < h; y += step) canvas.drawLine(0, y, w, y, line);
        canvas.save();
        canvas.translate(0, scanY);
        canvas.drawRect(0, 0, w, band, scan);
        canvas.restore();
    }
}
