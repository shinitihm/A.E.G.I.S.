package com.example.aegis.ui.heroes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.aegis.R;
import com.example.aegis.data.ApiCallback;
import com.example.aegis.data.ApiClient;
import com.example.aegis.data.LocalStore;
import com.example.aegis.data.model.CompareResult;
import com.example.aegis.data.model.HeroDetail;
import com.example.aegis.data.model.Threat;
import com.example.aegis.ui.hud.Hud;
import com.example.aegis.ui.hud.Speaker;
import com.example.aegis.ui.hud.StatRadarView;
import com.example.aegis.ui.hud.StateView;

import java.util.List;

/** Simulador de combate: "Hulk vs Thor" com o veredito da J.A.R.V.I.S. */
public class CompareActivity extends AppCompatActivity {

    private static final String EXTRA_A = "a";
    private static final String EXTRA_B = "b";
    private static final String[] AXES = {"PODERES", "APARIÇÕES", "INIMIGOS", "TIMES", "CÓSMICO"};

    public static Intent intent(Context context, String a, String b) {
        return new Intent(context, CompareActivity.class).putExtra(EXTRA_A, a).putExtra(EXTRA_B, b);
    }

    private EditText inputA;
    private EditText inputB;
    private StateView state;
    private View result;
    private Speaker speaker;
    private LocalStore store;
    private String lastVerdict;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Hud.setContent(this, R.layout.activity_compare);
        store = new LocalStore(this);
        speaker = new Speaker(this);
        inputA = findViewById(R.id.inputA);
        inputB = findViewById(R.id.inputB);
        state = findViewById(R.id.state);
        result = findViewById(R.id.result);

        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.simulate).setOnClickListener(v -> simulate());
        findViewById(R.id.hear).setOnClickListener(v -> {
            if (lastVerdict != null) speaker.speak(lastVerdict);
        });
        inputB.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) {
                simulate();
                return true;
            }
            return false;
        });

        String a = getIntent().getStringExtra(EXTRA_A);
        String b = getIntent().getStringExtra(EXTRA_B);
        if (a != null && b != null) {
            inputA.setText(a);
            inputB.setText(b);
            simulate();
        }
    }

    private void simulate() {
        String a = inputA.getText().toString().trim();
        String b = inputB.getText().toString().trim();
        if (a.isEmpty() || b.isEmpty()) return;

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(inputB.getWindowToken(), 0);
        speaker.stop();
        result.setVisibility(View.GONE);
        state.showLoading(getString(R.string.simulating));

        ApiClient.get().compare(a, b).enqueue(new ApiCallback<CompareResult>() {
            @Override
            public void onSuccess(CompareResult r) {
                if (isDestroyed()) return;
                state.hide();
                show(r);
            }

            @Override
            public void onError(String message) {
                if (isDestroyed()) return;
                state.showError(message, CompareActivity.this::simulate);
            }
        });
    }

    private void show(CompareResult r) {
        store.saveThreat(r.a.id, r.a.threat);
        store.saveThreat(r.b.id, r.b.threat);
        result.setVisibility(View.VISIBLE);
        lastVerdict = r.verdict;

        bindHero(r.a, R.id.photoA, R.id.nameA, R.id.threatA, r.winnerId != null && r.winnerId == r.a.id);
        bindHero(r.b, R.id.photoB, R.id.nameB, R.id.threatB, r.winnerId != null && r.winnerId == r.b.id);

        ((StatRadarView) findViewById(R.id.radar)).setData(AXES, normalized(r.a.threat), getColor(R.color.aegis_cyan),
                normalized(r.b.threat), getColor(R.color.aegis_gold));

        TextView probability = findViewById(R.id.probability);
        probability.setText(r.a.name.toUpperCase() + " " + r.probabilityA + "%   ·   "
                + (100 - r.probabilityA) + "% " + r.b.name.toUpperCase());
        ProgressBar bar = findViewById(R.id.probabilityBar);
        bar.setProgress(0);
        bar.setProgress(r.probabilityA, true);

        Hud.typewriter(findViewById(R.id.verdict), r.verdict);
        Hud.vibrate(this, 80);
    }

    private void bindHero(HeroDetail h, int photo, int name, int threat, boolean winner) {
        Glide.with(this).load(h.thumbUrl != null ? h.thumbUrl : h.imageUrl).into((ImageView) findViewById(photo));
        ((TextView) findViewById(name)).setText(h.name.toUpperCase() + (winner ? " ★" : ""));
        TextView t = findViewById(threat);
        t.setText(h.threat.level + " · " + h.threat.score);
        t.setTextColor(Hud.threatColor(this, h.threat.levelCode));
        findViewById(photo).setAlpha(winner ? 1f : 0.7f);
    }

    /** Pontos de cada fator convertidos para 0–100 (a ordem casa com AXES). */
    private static int[] normalized(Threat threat) {
        List<Threat.ThreatFactor> f = threat.factors;
        int[] out = new int[AXES.length];
        for (int i = 0; i < AXES.length && i < f.size(); i++) {
            out[i] = f.get(i).max == 0 ? 0 : f.get(i).points * 100 / f.get(i).max;
        }
        return out;
    }

    @Override
    protected void onDestroy() {
        speaker.shutdown();
        super.onDestroy();
    }
}
