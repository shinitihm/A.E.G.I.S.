package com.example.aegis.ui.hud;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.example.aegis.R;

/** Barra segmentada de 0 a 100 que "enche" quando o alvo é escaneado. */
public class ThreatMeterView extends View {

    private static final int SEGMENTS = 20;
    private final Paint on = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint off = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float gap;
    private float shown; // score animado

    public ThreatMeterView(Context context) {
        this(context, null);
    }

    public ThreatMeterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gap = Hud.dp(context, 2);
        int accent = ContextCompat.getColor(context, R.color.aegis_cyan);
        off.setColor(ColorUtils.setAlphaComponent(accent, 0x22));
        on.setColor(accent);
    }

    public void setThreat(int score, int color, boolean animate) {
        on.setColor(color);
        if (!animate) {
            shown = score;
            invalidate();
            return;
        }
        ValueAnimator anim = ValueAnimator.ofFloat(0f, score);
        anim.setDuration(1200);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            shown = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = resolveSize((int) Hud.dp(getContext(), 10), heightSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float segW = (getWidth() - gap * (SEGMENTS - 1)) / SEGMENTS;
        int filled = Math.round(shown / (100f / SEGMENTS));
        for (int i = 0; i < SEGMENTS; i++) {
            float x = i * (segW + gap);
            canvas.drawRect(x, 0, x + segW, getHeight(), i < filled ? on : off);
        }
    }
}
