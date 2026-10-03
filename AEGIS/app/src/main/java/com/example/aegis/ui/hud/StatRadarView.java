package com.example.aegis.ui.hud;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.aegis.R;

/** Gráfico radar de até 2 séries (usado nas armaduras e no comparador). */
public class StatRadarView extends View {

    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    private String[] labels = new String[0];
    private int[] seriesA = new int[0];
    private int[] seriesB;
    private int colorA;
    private int colorB;

    public StatRadarView(Context context) {
        this(context, null);
    }

    public StatRadarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        colorA = ContextCompat.getColor(context, R.color.aegis_cyan);
        colorB = ContextCompat.getColor(context, R.color.aegis_gold);
        grid.setStyle(Paint.Style.STROKE);
        grid.setColor(ContextCompat.getColor(context, R.color.aegis_cyan_dim));
        grid.setStrokeWidth(Hud.dp(context, 1));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Hud.dp(context, 2));
        stroke.setStrokeJoin(Paint.Join.ROUND);
        text.setColor(ContextCompat.getColor(context, R.color.aegis_text_dim));
        text.setTextSize(Hud.dp(context, 10));
        text.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(String[] labels, int[] a, int colorA, int[] b, int colorB) {
        this.labels = labels;
        this.seriesA = a;
        this.colorA = colorA;
        this.seriesB = b;
        this.colorB = colorB;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int n = labels.length;
        if (n < 3) return;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(cx, cy) - Hud.dp(getContext(), 22);

        for (int ring = 1; ring <= 4; ring++) {
            polygon(canvas, cx, cy, n, r * ring / 4f, grid);
        }
        for (int i = 0; i < n; i++) {
            double angle = angle(i, n);
            canvas.drawLine(cx, cy, cx + (float) Math.cos(angle) * r, cy + (float) Math.sin(angle) * r, grid);
            canvas.drawText(labels[i],
                    cx + (float) Math.cos(angle) * (r + Hud.dp(getContext(), 13)),
                    cy + (float) Math.sin(angle) * (r + Hud.dp(getContext(), 13)) + Hud.dp(getContext(), 3),
                    text);
        }
        draw(canvas, cx, cy, r, seriesA, colorA);
        if (seriesB != null) draw(canvas, cx, cy, r, seriesB, colorB);
    }

    private void draw(Canvas canvas, float cx, float cy, float r, int[] values, int color) {
        int n = labels.length;
        path.reset();
        for (int i = 0; i < n; i++) {
            double angle = angle(i, n);
            float dist = r * Math.max(0, Math.min(100, values[i])) / 100f;
            float x = cx + (float) Math.cos(angle) * dist;
            float y = cy + (float) Math.sin(angle) * dist;
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
        fill.setStyle(Paint.Style.FILL);
        fill.setColor((color & 0x00FFFFFF) | 0x40000000);
        canvas.drawPath(path, fill);
        stroke.setColor(color);
        canvas.drawPath(path, stroke);
    }

    private void polygon(Canvas canvas, float cx, float cy, int n, float radius, Paint paint) {
        path.reset();
        for (int i = 0; i < n; i++) {
            double angle = angle(i, n);
            float x = cx + (float) Math.cos(angle) * radius;
            float y = cy + (float) Math.sin(angle) * radius;
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
        canvas.drawPath(path, paint);
    }

    private static double angle(int i, int n) {
        return -Math.PI / 2 + i * 2 * Math.PI / n;
    }
}
