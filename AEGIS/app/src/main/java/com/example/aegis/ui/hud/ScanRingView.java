package com.example.aegis.ui.hud;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Anéis girando: usado no scanner de digital, na busca e nos carregamentos. */
public class ScanRingView extends View {

    private final Paint dashed = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcInner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private float rotation;
    private boolean scanning = true;
    private ValueAnimator animator;

    public ScanRingView(Context context) {
        this(context, null);
    }

    public ScanRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dashed.setStyle(Paint.Style.STROKE);
        dashed.setStrokeWidth(Hud.dp(context, 1.5f));
        dashed.setPathEffect(new DashPathEffect(new float[]{Hud.dp(context, 3), Hud.dp(context, 5)}, 0));
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(Hud.dp(context, 4));
        arc.setStrokeCap(Paint.Cap.ROUND);
        arcInner.setStyle(Paint.Style.STROKE);
        arcInner.setStrokeWidth(Hud.dp(context, 2));
        arcInner.setStrokeCap(Paint.Cap.ROUND);
        setColor(0xFF00E5FF);
    }

    public void setColor(int color) {
        arc.setColor(color);
        arcInner.setColor((color & 0x00FFFFFF) | 0xAA000000);
        dashed.setColor((color & 0x00FFFFFF) | 0x55000000);
        invalidate();
    }

    public void setScanning(boolean scanning) {
        this.scanning = scanning;
        if (animator != null) {
            if (scanning) animator.resume();
            else animator.pause();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(1800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            rotation = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
        if (!scanning) animator.pause();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(cx, cy) - arc.getStrokeWidth();

        canvas.drawCircle(cx, cy, r, dashed);

        oval.set(cx - r, cy - r, cx + r, cy + r);
        canvas.drawArc(oval, rotation, 70, false, arc);
        canvas.drawArc(oval, rotation + 180, 70, false, arc);

        float ri = r * 0.82f;
        oval.set(cx - ri, cy - ri, cx + ri, cy + ri);
        canvas.drawArc(oval, -rotation * 1.5f, 110, false, arcInner);
    }
}
