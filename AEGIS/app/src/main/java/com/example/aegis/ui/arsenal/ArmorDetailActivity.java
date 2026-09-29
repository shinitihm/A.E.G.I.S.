package com.example.aegis.ui.arsenal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.aegis.R;
import com.example.aegis.data.ApiCallback;
import com.example.aegis.data.ApiClient;
import com.example.aegis.data.model.Armor;
import com.example.aegis.ui.hud.Hud;
import com.example.aegis.ui.hud.IronHelmetView;
import com.example.aegis.ui.hud.ScanRingView;
import com.example.aegis.ui.hud.StatRadarView;
import com.example.aegis.ui.hud.StateView;
import com.google.android.material.button.MaterialButton;

/** Ficha da armadura: pedestal, gráfico radar de stats, armamentos e botão DEPLOY. */
public class ArmorDetailActivity extends AppCompatActivity {

    private static final String EXTRA_MARK = "mark";
    private static final String[] AXES = {"PODER", "BLINDAGEM", "VELOCIDADE", "VOO", "TECNOLOGIA"};

    public static Intent intent(Context context, int mark) {
        return new Intent(context, ArmorDetailActivity.class).putExtra(EXTRA_MARK, mark);
    }

    private StateView state;
    private View scroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Hud.setContent(this, R.layout.activity_armor_detail);
        state = findViewById(R.id.state);
        scroll = findViewById(R.id.scroll);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        load();
    }

    private void load() {
        scroll.setVisibility(View.INVISIBLE);
        state.showLoading(getString(R.string.loading_targets));
        ApiClient.get().armor(getIntent().getIntExtra(EXTRA_MARK, 1)).enqueue(new ApiCallback<Armor>() {
            @Override
            public void onSuccess(Armor armor) {
                if (isDestroyed()) return;
                state.hide();
                bind(armor);
            }

            @Override
            public void onError(String message) {
                if (isDestroyed()) return;
                state.showError(message, ArmorDetailActivity.this::load);
            }
        });
    }

    private void bind(Armor a) {
        scroll.setVisibility(View.VISIBLE);
        int primary = Hud.parseColor(a.primaryColor, 0xFFB71C1C);
        int secondary = Hud.parseColor(a.secondaryColor, 0xFFFFB300);

        IronHelmetView helmet = findViewById(R.id.helmet);
        helmet.setColors(primary, secondary);
        if (a.imageUrl != null) { // a Comic Vine às vezes tem a arte da armadura
            helmet.setVisibility(View.GONE);
            ImageView photo = findViewById(R.id.photo);
            photo.setVisibility(View.VISIBLE);
            Glide.with(this).load(a.imageUrl).into(photo);
        }

        ((TextView) findViewById(R.id.code)).setText(a.code);
        ((TextView) findViewById(R.id.nickname)).setText(a.nickname != null ? a.nickname.toUpperCase() : "");
        ((TextView) findViewById(R.id.meta)).setText(a.debut + "\n" + a.armorClass.toUpperCase());
        TextView status = findViewById(R.id.status);
        status.setText(a.status);
        status.setTextColor(Hud.statusColor(this, a.status));
        ((TextView) findViewById(R.id.description)).setText(a.description);

        Armor.Stats s = a.stats;
        ((StatRadarView) findViewById(R.id.radar)).setData(AXES,
                new int[]{s.power, s.armor, s.speed, s.flight, s.tech}, secondaryOrCyan(secondary), null, 0);

        LinearLayout weapons = findViewById(R.id.weapons);
        weapons.removeAllViews();
        for (String weapon : a.weapons) {
            TextView row = new TextView(this);
            row.setTextAppearance(R.style.Aegis_Mono);
            row.setText("▸ " + weapon);
            row.setPadding(0, (int) Hud.dp(this, 3), 0, (int) Hud.dp(this, 3));
            weapons.addView(row);
        }

        MaterialButton deploy = findViewById(R.id.deploy);
        deploy.setOnClickListener(v -> deploy(a, deploy));
    }

    /** Cores escuras demais somem no fundo preto; usa ciano nesses casos. */
    private int secondaryOrCyan(int color) {
        float brightness = (android.graphics.Color.red(color) + android.graphics.Color.green(color)
                + android.graphics.Color.blue(color)) / 765f;
        return brightness < 0.3f ? getColor(R.color.aegis_cyan) : color;
    }

    private void deploy(Armor armor, MaterialButton button) {
        button.setEnabled(false);
        button.setText(R.string.deployed);
        Hud.vibrate(this, 150);
        Hud.flash(findViewById(R.id.flash), 0x33FFB300);

        ScanRingView ring = findViewById(R.id.deployRing);
        ring.setColor(getColor(R.color.aegis_gold));
        ring.setVisibility(View.VISIBLE);
        ring.setAlpha(1f);
        ring.animate().alpha(0f).setStartDelay(1500).setDuration(600)
                .withEndAction(() -> {
                    ring.setVisibility(View.GONE);
                    button.setEnabled(true);
                    button.setText(R.string.deploy);
                }).start();
    }
}
