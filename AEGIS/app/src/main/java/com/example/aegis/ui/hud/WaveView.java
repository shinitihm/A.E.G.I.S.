package com.example.aegis.ui.hud;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Onda de áudio animada: aparece quando a J.A.R.V.I.S. está pensando ou falando. */
public class WaveView extends View {

    private static final int BARS = 28;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase;
    private ValueAnimator animator;

    public WaveView(Context context) {
        this(context, null);
    }

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(0xFF00E5FF);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Hud.dp(context, 3));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        animator.setDuration(1100);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
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
        float w = getWidth();
        float h = getHeight();
        float slot = w / BARS;
        for (int i = 0; i < BARS; i++) {
            double envelope = Math.sin(Math.PI * (i + 0.5) / BARS); // mais alto no meio
            double wave = 0.5 + 0.5 * Math.sin(phase + i * 0.55) * Math.sin(phase * 0.7 + i * 0.3);
            float bar = (float) (h * (0.12 + 0.88 * envelope * Math.abs(wave)));
            float x = slot * (i + 0.5f);
            canvas.drawLine(x, (h - bar) / 2f, x, (h + bar) / 2f, paint);
        }
    }
}
