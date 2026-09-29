package com.example.aegis.ui.hud;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Silhueta do capacete do Homem de Ferro desenhada em código, colorida por armadura.
 * (A Comic Vine quase não tem imagens de armaduras, então o Arsenal usa isto como pedestal.)
 */
public class IronHelmetView extends View {

    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint faceplate = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyes = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pedestal = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path helmet = new Path();
    private final Path face = new Path();
    private final RectF rect = new RectF();

    public IronHelmetView(Context context) {
        this(context, null);
    }

    public IronHelmetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        outline.setStyle(Paint.Style.STROKE);
        outline.setColor(0xAA000000);
        outline.setStrokeWidth(Hud.dp(context, 1.5f));
        eyes.setColor(0xFFE0FFFF);
        pedestal.setStyle(Paint.Style.STROKE);
        pedestal.setStrokeWidth(Hud.dp(context, 2));
        setColors(0xFFB71C1C, 0xFFFFB300);
    }

    public void setColors(int primary, int secondary) {
        body.setColor(primary);
        faceplate.setColor(secondary);
        pedestal.setColor((secondary & 0x00FFFFFF) | 0x99000000);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float s = Math.min(w, h * 0.9f);
        float cx = w / 2f;
        float top = h * 0.04f;

        // pedestal de luz
        for (int i = 0; i < 3; i++) {
            float pw = s * (0.42f + i * 0.13f);
            rect.set(cx - pw, h * 0.86f - pw * 0.13f, cx + pw, h * 0.86f + pw * 0.13f);
            pedestal.setAlpha(200 - i * 60);
            canvas.drawOval(rect, pedestal);
        }

        // casco
        float hw = s * 0.36f;
        helmet.reset();
        helmet.moveTo(cx, top);
        helmet.cubicTo(cx + hw * 1.15f, top, cx + hw * 1.05f, top + s * 0.42f, cx + hw * 0.9f, top + s * 0.62f);
        helmet.lineTo(cx + hw * 0.55f, top + s * 0.86f);
        helmet.lineTo(cx - hw * 0.55f, top + s * 0.86f);
        helmet.lineTo(cx - hw * 0.9f, top + s * 0.62f);
        helmet.cubicTo(cx - hw * 1.05f, top + s * 0.42f, cx - hw * 1.15f, top, cx, top);
        helmet.close();
        canvas.drawPath(helmet, body);
        canvas.drawPath(helmet, outline);

        // máscara facial
        face.reset();
        face.moveTo(cx - hw * 0.62f, top + s * 0.22f);
        face.lineTo(cx + hw * 0.62f, top + s * 0.22f);
        face.lineTo(cx + hw * 0.66f, top + s * 0.55f);
        face.lineTo(cx + hw * 0.36f, top + s * 0.82f);
        face.lineTo(cx - hw * 0.36f, top + s * 0.82f);
        face.lineTo(cx - hw * 0.66f, top + s * 0.55f);
        face.close();
        canvas.drawPath(face, faceplate);
        canvas.drawPath(face, outline);

        // olhos
        Path eye = new Path();
        float ey = top + s * 0.36f;
        eye.moveTo(cx - hw * 0.5f, ey);
        eye.lineTo(cx - hw * 0.1f, ey + s * 0.03f);
        eye.lineTo(cx - hw * 0.16f, ey + s * 0.075f);
        eye.lineTo(cx - hw * 0.5f, ey + s * 0.045f);
        eye.close();
        canvas.drawPath(eye, eyes);
        canvas.save();
        canvas.scale(-1, 1, cx, 0);
        canvas.drawPath(eye, eyes);
        canvas.restore();

        // boca / grade
        outline.setStrokeWidth(Hud.dp(getContext(), 1));
        for (int i = 0; i < 3; i++) {
            float y = top + s * (0.68f + i * 0.035f);
            canvas.drawLine(cx - hw * 0.2f, y, cx + hw * 0.2f, y, outline);
        }
        outline.setStrokeWidth(Hud.dp(getContext(), 1.5f));
    }
}
