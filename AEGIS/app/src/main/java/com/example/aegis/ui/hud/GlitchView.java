package com.example.aegis.ui.hud;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

import com.example.aegis.R;

import java.util.Random;

/**
 * Easter egg "Ultron": cobre a tela inteira com interferência (faixas deslocadas, texto tremendo)
 * por alguns segundos e depois se remove sozinha. Enquanto está na tela, bloqueia os toques.
 */
public class GlitchView extends View {

    public static final long DURATION = 2600;
    private static final int RED = 0xFFFF1744;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final long start = SystemClock.uptimeMillis();
    private final int accent;

    private GlitchView(Context context) {
        super(context);
        accent = ContextCompat.getColor(context, R.color.aegis_cyan); // segue o tema (Stark ou Doom)
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextAlign(Paint.Align.CENTER);
        setClickable(true); // "perdeu o controle": nada responde ao toque durante a invasão
    }

    /** Mostra o glitch por cima de tudo na Activity. */
    public static void show(Activity activity) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        root.addView(new GlitchView(activity), new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (SystemClock.uptimeMillis() - start > DURATION) {
            post(() -> {
                if (getParent() instanceof ViewGroup) ((ViewGroup) getParent()).removeView(this);
            });
            return;
        }
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        // véu vermelho pulsando
        canvas.drawColor(Color.argb(70 + random.nextInt(110), 110, 0, 10));

        // faixas de interferência em posições aleatórias a cada quadro
        for (int i = 0; i < 16; i++) {
            float top = random.nextInt(h);
            float height = 4 + random.nextInt(Math.max(1, h / 16));
            float shift = random.nextInt(Math.max(1, w / 3)) - w / 6f;
            paint.setColor(i % 3 == 0 ? RED : i % 3 == 1 ? accent : Color.BLACK);
            paint.setAlpha(120 + random.nextInt(120));
            canvas.drawRect(shift, top, shift + w, top + height, paint);
        }

        // nome tremendo, com as cores "descoladas"; some em 1 a cada 4 quadros para piscar
        if (random.nextInt(4) != 0) {
            float cx = w / 2f + random.nextInt(25) - 12;
            float cy = h / 2f + random.nextInt(25) - 12;
            paint.setTextSize(w / 5.5f);
            paint.setAlpha(255);
            paint.setColor(accent);
            canvas.drawText("ULTRON", cx - 10, cy, paint);
            paint.setColor(RED);
            canvas.drawText("ULTRON", cx + 10, cy, paint);
            paint.setColor(Color.WHITE);
            canvas.drawText("ULTRON", cx, cy, paint);
            paint.setTextSize(w / 24f);
            canvas.drawText("NÃO HÁ CORDAS EM MIM", cx, cy + w / 9f, paint);
        }
        postInvalidateDelayed(45); // ~20 quadros por segundo: o suficiente para parecer defeito
    }
}
