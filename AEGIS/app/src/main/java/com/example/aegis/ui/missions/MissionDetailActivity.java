package com.example.aegis.ui.missions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.aegis.R;
import com.example.aegis.data.ApiCallback;
import com.example.aegis.data.ApiClient;
import com.example.aegis.data.model.Mission;
import com.example.aegis.ui.hud.Hud;
import com.example.aegis.ui.hud.StateView;

/** Arquivo de missão: descrição do arco e as edições que o compõem. */
public class MissionDetailActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "mission_id";

    public static Intent intent(Context context, int id) {
        return new Intent(context, MissionDetailActivity.class).putExtra(EXTRA_ID, id);
    }

    private StateView state;
    private View scroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Hud.setContent(this, R.layout.activity_mission_detail);
        state = findViewById(R.id.state);
        scroll = findViewById(R.id.scroll);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        load();
    }

    private void load() {
        scroll.setVisibility(View.INVISIBLE);
        state.showLoading(getString(R.string.missions_loading));
        ApiClient.get().mission(getIntent().getIntExtra(EXTRA_ID, 0)).enqueue(new ApiCallback<Mission.Detail>() {
            @Override
            public void onSuccess(Mission.Detail m) {
                if (isDestroyed()) return;
                state.hide();
                bind(m);
            }

            @Override
            public void onError(String message) {
                if (isDestroyed()) return;
                state.showError(message, MissionDetailActivity.this::load);
            }
        });
    }

    private void bind(Mission.Detail m) {
        scroll.setVisibility(View.VISIBLE);
        Glide.with(this).load(m.imageUrl).into((ImageView) findViewById(R.id.cover));
        ((TextView) findViewById(R.id.name)).setText(m.name.toUpperCase());
        ((TextView) findViewById(R.id.meta)).setText(m.issueCount != null ? m.issueCount + " EDIÇÕES" : "");

        String text = m.description != null ? m.description : m.deck;
        ((TextView) findViewById(R.id.description)).setText(text != null ? text : getString(R.string.none_registered));
        ((TextView) findViewById(R.id.issues)).setText(m.issues == null || m.issues.isEmpty()
                ? getString(R.string.none_registered)
                : "▸ " + TextUtils.join("\n▸ ", m.issues));
    }
}
