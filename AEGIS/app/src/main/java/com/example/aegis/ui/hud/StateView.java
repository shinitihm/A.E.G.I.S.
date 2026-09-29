package com.example.aegis.ui.hud;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.aegis.R;

/** Estado de tela temático: carregando (radar), erro (toque para repetir) ou vazio. */
public class StateView extends FrameLayout {

    private final ScanRingView ring;
    private final TextView message;
    private final TextView hint;

    public StateView(Context context) {
        this(context, null);
    }

    public StateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        int pad = (int) Hud.dp(context, 24);
        box.setPadding(pad, pad, pad, pad);

        int size = (int) Hud.dp(context, 84);
        ring = new ScanRingView(context);
        box.addView(ring, new LinearLayout.LayoutParams(size, size));

        message = new TextView(context);
        message.setTextAppearance(R.style.Aegis_Heading);
        message.setGravity(Gravity.CENTER);
        message.setTextSize(13);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mp.topMargin = (int) Hud.dp(context, 14);
        box.addView(message, mp);

        hint = new TextView(context);
        hint.setTextAppearance(R.style.Aegis_Label);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hp.topMargin = (int) Hud.dp(context, 6);
        box.addView(hint, hp);

        addView(box, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        setVisibility(GONE);
    }

    public void showLoading(String text) {
        show(ContextCompat.getColor(getContext(), R.color.aegis_cyan), true, text, "");
        setOnClickListener(null);
        setClickable(false);
    }

    public void showError(String text, Runnable retry) {
        show(ContextCompat.getColor(getContext(), R.color.aegis_red), false, text,
                getContext().getString(R.string.tap_retry));
        setOnClickListener(v -> retry.run());
    }

    public void showEmpty(String text) {
        show(ContextCompat.getColor(getContext(), R.color.aegis_text_dim), false, text, "");
        setOnClickListener(null);
        setClickable(false);
    }

    public void hide() {
        setVisibility(GONE);
    }

    private void show(int color, boolean scanning, String text, String hintText) {
        ring.setColor(color);
        ring.setScanning(scanning);
        message.setText(text);
        message.setTextColor(color);
        hint.setText(hintText);
        hint.setVisibility(hintText.isEmpty() ? View.GONE : View.VISIBLE);
        setVisibility(VISIBLE);
    }
}
