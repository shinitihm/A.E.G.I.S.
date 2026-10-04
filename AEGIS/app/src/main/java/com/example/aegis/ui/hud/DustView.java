package com.example.aegis.ui.hud;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import androidx.core.content.ContextCompat;

import com.example.aegis.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Easter egg "snap": cada card escolhido se desfaz em pó da esquerda para a direita, um depois do outro.
 * No lugar dele fica o nome do alvo com "SINAL PERDIDO"; alguns segundos depois os cards voltam.
 *
 * O card de verdade é "comido" por um recorte (clipBounds) que avança; esta view, por cima da tela inteira,
 * desenha só as partículas e os nomes. Enquanto o efeito roda, ela bloqueia os toques.
 */
public class DustView extends View {

    private static final long STAGGER = 260;  // intervalo entre o início de um card e o do próximo
    private static final long SWEEP = 850;    // tempo para o pó atravessar um card
    private static final long LIFE = 1200;    // vida máxima de uma partícula
    private static final long HOLD = 1900;    // tempo com os nomes na tela antes de os cards voltarem
    private static final long RESTORE = 650;  // volta dos cards

    private static final class Victim {
        View card;
        String name;
        float left, top;
        int w, h, count;
        float[] x, y, vx, vy, life; // partículas: posição inicial (no card), velocidade (px/s) e vida (ms)
        int[] color;
        boolean started;
        final Rect clip = new Rect();
    }

    private final List<Victim> victims = new ArrayList<>();
    private final Paint dust = new Paint();
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ValueAnimator clock;
    private final Runnable onEnd;
    private final float density, cell;
    private final int red;
    private final long restoreAt, total;
    private long now;
    private boolean finished;

    /** Dispara o efeito sobre os cards informados. {@code onEnd} roda quando tudo voltou ao normal. */
    public static void snap(Activity activity, List<View> cards, List<String> names, Runnable onEnd) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        DustView view = new DustView(activity, root, cards, names, onEnd);
        root.addView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        view.clock.start();
    }

    private DustView(Activity activity, ViewGroup root, List<View> cards, List<String> names, Runnable onEnd) {
        super(activity);
        this.onEnd = onEnd;
        density = getResources().getDisplayMetrics().density;
        cell = 4 * density;
        red = ContextCompat.getColor(activity, R.color.aegis_red);
        int accent = ContextCompat.getColor(activity, R.color.aegis_cyan); // segue o tema (Stark ou Doom)
        text.setTypeface(Typeface.MONOSPACE);
        setClickable(true); // sem toques nem rolagem durante o efeito

        int[] origin = new int[2];
        root.getLocationInWindow(origin);
        Random random = new Random();
        for (int i = 0; i < cards.size(); i++) {
            victims.add(buildVictim(cards.get(i), names.get(i), origin, accent, random));
        }

        restoreAt = (victims.size() - 1) * STAGGER + SWEEP + LIFE + HOLD;
        total = restoreAt + RESTORE;
        clock = ValueAnimator.ofFloat(0f, 1f).setDuration(total);
        clock.setInterpolator(new LinearInterpolator());
        clock.addUpdateListener(a -> {
            now = a.getCurrentPlayTime();
            updateCards();
            invalidate();
        });
        clock.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finish();
            }
        });
    }

    private Victim buildVictim(View card, String name, int[] origin, int accent, Random random) {
        Victim v = new Victim();
        v.card = card;
        v.name = name;
        v.w = card.getWidth();
        v.h = card.getHeight();
        int[] pos = new int[2];
        card.getLocationInWindow(pos);
        v.left = pos[0] - origin[0];
        v.top = pos[1] - origin[1];

        // Foto do card só para escolher a cor de cada partícula. Imagens aceleradas por hardware (Glide)
        // não podem ser desenhadas num Canvas comum: se falhar, o pó sai todo na cor do tema.
        Bitmap shot = Bitmap.createBitmap(Math.max(1, v.w), Math.max(1, v.h), Bitmap.Config.ARGB_8888);
        try {
            card.draw(new Canvas(shot));
        } catch (RuntimeException ignored) {
            shot.eraseColor(Color.TRANSPARENT);
        }

        int cols = (int) (v.w / cell), rows = (int) (v.h / cell);
        int max = Math.max(1, cols * rows);
        v.x = new float[max];
        v.y = new float[max];
        v.vx = new float[max];
        v.vy = new float[max];
        v.life = new float[max];
        v.color = new int[max];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float px = (c + 0.5f) * cell, py = (r + 0.5f) * cell;
                int pixel = shot.getPixel((int) px, (int) py);
                int light = (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000;
                boolean dark = Color.alpha(pixel) < 128 || light < 70;
                if (dark && random.nextInt(3) != 0) continue; // fundo escuro: só 1 em 3 vira partícula
                int k = v.count++;
                v.x[k] = px;
                v.y[k] = py;
                v.vx[k] = (30 + random.nextInt(110)) * density;
                v.vy[k] = -(20 + random.nextInt(120)) * density;
                v.life[k] = LIFE * (0.55f + random.nextFloat() * 0.45f);
                v.color[k] = dark ? accent : pixel;
            }
        }
        shot.recycle();
        return v;
    }

    /** Avança o recorte que "come" cada card e, no fim, traz os cards de volta. */
    private void updateCards() {
        for (int i = 0; i < victims.size(); i++) {
            Victim v = victims.get(i);
            long local = now - i * STAGGER;
            if (local < 0) continue;
            if (!v.started) {
                v.started = true;
                Hud.vibrate(getContext(), 35);
            }
            if (now < restoreAt) {
                int front = (int) (Math.min(1f, local / (float) SWEEP) * v.w);
                v.clip.set(front, 0, v.w, v.h);
                v.card.setClipBounds(v.clip);
                if (front >= v.w) v.card.setAlpha(0f); // sumiu por inteiro: a volta parte do invisível
            } else {
                v.card.setClipBounds(null);
                v.card.setAlpha((now - restoreAt) / (float) RESTORE);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float back = now < restoreAt ? 0f : Math.min(1f, (now - restoreAt) / (float) RESTORE);
        for (int i = 0; i < victims.size(); i++) {
            Victim v = victims.get(i);
            long local = now - i * STAGGER;
            if (local < 0) continue;

            // quem sumiu: nome + "SINAL PERDIDO" no lugar do card
            float shown = Math.min(1f, local / (float) SWEEP) * (1f - back);
            text.setColor(red);
            text.setAlpha((int) (255 * shown));
            text.setTextSize(17 * density);
            text.setFakeBoldText(true);
            canvas.drawText(v.name, v.left + 16 * density, v.top + v.h / 2f - 2 * density, text);
            text.setTextSize(11 * density);
            text.setFakeBoldText(false);
            canvas.drawText("● SINAL PERDIDO", v.left + 16 * density, v.top + v.h / 2f + 16 * density, text);

            // pó: cada partícula nasce quando a frente do recorte passa por ela
            for (int k = 0; k < v.count; k++) {
                float age = local - v.x[k] / v.w * SWEEP;
                if (age < 0 || age > v.life[k]) continue;
                float p = age / v.life[k], s = age / 1000f;
                float px = v.left + v.x[k] + v.vx[k] * s;
                float py = v.top + v.y[k] + v.vy[k] * s * (0.4f + p); // sobe cada vez mais rápido
                float half = cell * (1f - 0.6f * p) / 2f;
                dust.setColor(v.color[k]);
                dust.setAlpha((int) (230 * (1f - p)));
                canvas.drawRect(px - half, py - half, px + half, py + half, dust);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clock.cancel(); // tela recriada no meio do efeito: garante que os cards voltem
        finish();
    }

    private void finish() {
        if (finished) return;
        finished = true;
        for (Victim v : victims) {
            v.card.setClipBounds(null);
            v.card.setAlpha(1f);
        }
        post(() -> {
            if (getParent() instanceof ViewGroup) ((ViewGroup) getParent()).removeView(this);
        });
        onEnd.run();
    }
}
