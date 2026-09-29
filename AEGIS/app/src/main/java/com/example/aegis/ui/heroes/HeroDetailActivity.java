package com.example.aegis.ui.heroes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.aegis.R;
import com.example.aegis.data.ApiCallback;
import com.example.aegis.data.ApiClient;
import com.example.aegis.data.LocalStore;
import com.example.aegis.data.model.HeroDetail;
import com.example.aegis.data.model.Threat;
import com.example.aegis.ui.hud.Hud;
import com.example.aegis.ui.hud.ScanRingView;
import com.example.aegis.ui.hud.StateView;
import com.example.aegis.ui.hud.ThreatMeterView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

/** Dossiê do alvo: foto com scanner, ameaça calculada, poderes, times, inimigos e aliados. */
public class HeroDetailActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "hero_id";
    public static final String RESULT_ASK = "ask_jarvis_about";

    public static Intent intent(Context context, int heroId) {
        return new Intent(context, HeroDetailActivity.class).putExtra(EXTRA_ID, heroId);
    }

    private LocalStore store;
    private StateView state;
    private ScrollView scroll;
    private HeroDetail hero;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Hud.setContent(this, R.layout.activity_hero_detail);
        store = new LocalStore(this);
        state = findViewById(R.id.state);
        scroll = findViewById(R.id.scroll);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        load();
    }

    private void load() {
        scroll.setVisibility(View.INVISIBLE);
        state.showLoading(getString(R.string.scanning_target));
        ApiClient.get().hero(getIntent().getIntExtra(EXTRA_ID, 0)).enqueue(new ApiCallback<HeroDetail>() {
            @Override
            public void onSuccess(HeroDetail body) {
                if (isDestroyed()) return;
                hero = body;
                store.saveThreat(body.id, body.threat);
                state.hide();
                bind(body);
            }

            @Override
            public void onError(String message) {
                if (isDestroyed()) return;
                state.showError(message, HeroDetailActivity.this::load);
            }
        });
    }

    private void bind(HeroDetail h) {
        scroll.setVisibility(View.VISIBLE);
        Glide.with(this).load(h.imageUrl).into((ImageView) findViewById(R.id.photo));

        // o anel de scan gira sobre a foto por um instante e some
        ScanRingView ring = findViewById(R.id.photoRing);
        ring.setColor(Hud.threatColor(this, h.threat.levelCode));
        ring.animate().alpha(0f).setStartDelay(1400).setDuration(600)
                .withEndAction(() -> ring.setVisibility(View.GONE)).start();

        ((TextView) findViewById(R.id.publisher)).setText(h.publisher != null ? h.publisher.toUpperCase() : "");
        ((TextView) findViewById(R.id.name)).setText(h.name.toUpperCase());
        ((TextView) findViewById(R.id.realName)).setText(h.realName != null ? h.realName : "");

        ((TextView) findViewById(R.id.statAppearances)).setText(Hud.number(h.appearances));
        ((TextView) findViewById(R.id.statTeams)).setText(String.valueOf(h.teams.size()));
        ((TextView) findViewById(R.id.statFirst)).setText(h.firstAppearance != null ? h.firstAppearance : "—");

        bindThreat(h.threat);

        TextView bio = findViewById(R.id.bio);
        String text = h.bio != null ? h.bio : h.deck;
        bio.setText(text != null ? text : getString(R.string.none_registered));

        fillChips(findViewById(R.id.powers), h.powers, true);
        fillChips(findViewById(R.id.teamChips), h.teams, false);
        setList(findViewById(R.id.enemies), h.enemies);
        setList(findViewById(R.id.allies), h.friends);

        MaterialButton monitor = findViewById(R.id.monitor);
        updateMonitor(monitor, store.isMonitored(h.id));
        monitor.setOnClickListener(v -> updateMonitor(monitor, store.toggleMonitored(h)));

        findViewById(R.id.askJarvis).setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.aegis.MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(RESULT_ASK, h.name));
            finish();
        });
    }

    private void bindThreat(Threat t) {
        int color = Hud.threatColor(this, t.levelCode);
        TextView level = findViewById(R.id.threatLevel);
        level.setText(t.level);
        level.setTextColor(color);
        TextView score = findViewById(R.id.threatScore);
        score.setText(t.score + " / 100");
        score.setTextColor(color);
        ThreatMeterView meter = findViewById(R.id.threatMeter);
        meter.setThreat(t.score, color, true);
        if ("OMEGA".equals(t.levelCode)) {
            level.animate().alpha(0.35f).setDuration(600).withEndAction(new Runnable() {
                @Override
                public void run() { // pulsa enquanto a tela estiver aberta
                    if (isDestroyed()) return;
                    boolean dim = level.getAlpha() > 0.7f;
                    level.animate().alpha(dim ? 0.35f : 1f).setDuration(600).withEndAction(this).start();
                }
            }).start();
        }

        LinearLayout factors = findViewById(R.id.factors);
        factors.removeAllViews();
        for (Threat.ThreatFactor f : t.factors) {
            TextView row = new TextView(this);
            row.setTextAppearance(R.style.Aegis_Mono);
            row.setTextSize(12);
            row.setText(String.format("%-14s %2d/%d", f.label, f.points, f.max));
            row.setTextColor(f.points > 0 ? getColor(R.color.aegis_text) : getColor(R.color.aegis_text_dim));
            factors.addView(row, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private void fillChips(ChipGroup group, List<String> items, boolean limit) {
        group.removeAllViews();
        if (items == null || items.isEmpty()) {
            TextView none = new TextView(this);
            none.setTextAppearance(R.style.Aegis_Mono);
            none.setText(R.string.none_registered);
            group.addView(none);
            return;
        }
        int max = limit ? 14 : 12;
        for (int i = 0; i < Math.min(max, items.size()); i++) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_info, group, false);
            chip.setText(items.get(i));
            group.addView(chip);
        }
    }

    private void setList(TextView view, List<String> items) {
        String joined = Hud.join(items, 10);
        view.setText(joined != null ? joined : getString(R.string.none_registered));
    }

    private void updateMonitor(MaterialButton button, boolean monitored) {
        button.setText(monitored ? R.string.monitoring : R.string.monitor);
    }
}
