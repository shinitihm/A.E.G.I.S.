package com.example.aegis.ui.hud;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.aegis.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Utilitários visuais compartilhados por todas as telas. */
public final class Hud {

    private Hud() {
    }

    /** setContentView + tela cheia (edge-to-edge) com padding para as barras do sistema e o teclado. */
    public static void setContent(AppCompatActivity activity, @LayoutRes int layout) {
        EdgeToEdge.enable(activity,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT));
        activity.setContentView(layout);
        View root = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    public static int threatColor(Context context, String levelCode) {
        int res;
        if ("OMEGA".equals(levelCode)) res = R.color.threat_omega;
        else if ("HIGH".equals(levelCode)) res = R.color.threat_high;
        else if ("MODERATE".equals(levelCode)) res = R.color.threat_moderate;
        else res = R.color.threat_low;
        return ContextCompat.getColor(context, res);
    }

    /** Cor do status de uma armadura (ATIVA, DESTRUÍDA...). */
    public static int statusColor(Context context, String status) {
        int res;
        if (status == null) res = R.color.aegis_text_dim;
        else if (status.startsWith("DESTRU")) res = R.color.aegis_red;
        else if (status.startsWith("DANIF")) res = R.color.threat_high;
        else if (status.startsWith("LEND")) res = R.color.aegis_gold;
        else if (status.startsWith("CONVERT")) res = R.color.aegis_text_dim;
        else res = R.color.aegis_cyan;
        return ContextCompat.getColor(context, res);
    }

    public static int parseColor(String hex, int fallback) {
        try {
            return Color.parseColor(hex);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    public static String number(int value) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(value);
    }

    public static String join(List<String> items, int max) {
        if (items == null || items.isEmpty()) return null;
        List<String> shown = items.subList(0, Math.min(max, items.size()));
        String text = TextUtils.join(" · ", shown);
        return items.size() > max ? text + "  +" + (items.size() - max) : text;
    }

    public static float dp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }

    @SuppressWarnings("deprecation")
    public static void vibrate(Context context, long millis) {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager manager = context.getSystemService(VibratorManager.class);
            vibrator = manager != null ? manager.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(millis);
        }
    }

    /** Escreve o texto letra por letra, estilo terminal. */
    public static void typewriter(TextView view, CharSequence text) {
        Object previous = view.getTag(R.id.tag_typewriter);
        if (previous instanceof Runnable) view.removeCallbacks((Runnable) previous);

        long step = Math.max(8, Math.min(30, 1500 / Math.max(1, text.length())));
        Runnable writer = new Runnable() {
            int shown = 0;

            @Override
            public void run() {
                shown++;
                view.setText(text.subSequence(0, Math.min(shown, text.length())));
                if (shown < text.length()) view.postDelayed(this, step);
            }
        };
        view.setTag(R.id.tag_typewriter, writer);
        view.setText("");
        view.post(writer);
    }

    /** Pisca a tela inteira com uma cor (usado nos easter eggs). */
    public static void flash(View overlay, int color) {
        overlay.setBackgroundColor(color);
        overlay.setVisibility(View.VISIBLE);
        overlay.setAlpha(0.85f);
        overlay.animate().alpha(0f).setDuration(1100)
                .withEndAction(() -> overlay.setVisibility(View.GONE))
                .start();
    }
}
