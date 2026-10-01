package com.example.aegis.ui.hud;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.example.aegis.R;

/** O reator arc do peito do Tony, girando e pulsando. */
public class ArcReactorView extends View {

    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint segment = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint triangle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint core = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Path trianglePath = new Path();
    private final int accent;
    private final int light;
    private float rotation;
    private float pulse = 1f;
    private ValueAnimator animator;

    public ArcReactorView(Context context) {
        this(context, null);
    }

    public ArcReactorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        accent = ContextCompat.getColor(context, R.color.aegis_cyan);
        light = ContextCompat.getColor(context, R.color.aegis_cyan_light);
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(accent);
        segment.setStyle(Paint.Style.STROKE);
        segment.setColor(ColorUtils.setAlphaComponent(accent, 0xCC));
        segment.setStrokeCap(Paint.Cap.BUTT);
        triangle.setStyle(Paint.Style.STROKE);
        triangle.setColor(light);
        triangle.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float r = Math.min(w, h) / 2f;
        float cx = w / 2f;
        float cy = h / 2f;
        glow.setShader(new RadialGradient(cx, cy, r,
                new int[]{ColorUtils.setAlphaComponent(accent, 0x66), ColorUtils.setAlphaComponent(accent, 0x22),
                        ColorUtils.setAlphaComponent(accent, 0x00)},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        core.setShader(new RadialGradient(cx, cy, r * 0.3f,
                new int[]{Color.WHITE, light, ColorUtils.setAlphaComponent(accent, 0x00)},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        ring.setStrokeWidth(r * 0.05f);
        segment.setStrokeWidth(r * 0.12f);
        triangle.setStrokeWidth(r * 0.035f);

        float t = r * 0.42f;
        trianglePath.reset();
        trianglePath.moveTo(cx, cy - t);
        trianglePath.lineTo(cx + t * 0.866f, cy + t * 0.5f);
        trianglePath.lineTo(cx - t * 0.866f, cy + t * 0.5f);
        trianglePath.close();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(3000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            rotation = f * 360f;
            pulse = 0.88f + 0.12f * (float) Math.sin(f * Math.PI * 4);
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
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(getWidth(), getHeight()) / 2f;

        glow.setAlpha((int) (255 * pulse));
        canvas.drawCircle(cx, cy, r, glow);
        canvas.drawCircle(cx, cy, r * 0.82f, ring);

        canvas.save();
        canvas.rotate(rotation, cx, cy);
        float sr = r * 0.64f;
        oval.set(cx - sr, cy - sr, cx + sr, cy + sr);
        for (int i = 0; i < 10; i++) {
            canvas.drawArc(oval, i * 36f + 5f, 24f, false, segment);
        }
        canvas.restore();

        canvas.save();
        canvas.rotate(-rotation / 2f, cx, cy);
        canvas.drawPath(trianglePath, triangle);
        canvas.restore();

        canvas.drawCircle(cx, cy, r * 0.3f * pulse, core);
    }
}
