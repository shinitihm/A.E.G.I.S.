package com.example.aegis;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aegis.ui.hud.Hud;

/** Boot do sistema: reator, logo e log estilo terminal. Depois segue para a biometria. */
public class SplashActivity extends AppCompatActivity {

    private static final String[] BOOT_LINES = {
            "> INICIALIZANDO REATOR ARC ........ OK",
            "> CARREGANDO PROTOCOLOS DE ARMADURA  OK",
            "> CONECTANDO AO SATÉLITE STARK ..... OK",
            "> SINCRONIZANDO J.A.R.V.I.S. ....... OK",
            "> AGUARDANDO IDENTIFICAÇÃO"
    };
    private static final long LINE_DELAY_MS = 520;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView bootLog;
    private ProgressBar progress;
    private int line;
    private boolean finished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Hud.setContent(this, R.layout.activity_splash);

        bootLog = findViewById(R.id.bootLog);
        progress = findViewById(R.id.bootProgress);
        fadeIn(findViewById(R.id.reactor), 0);
        fadeIn(findViewById(R.id.logo), 400);
        fadeIn(findViewById(R.id.tagline), 800);

        handler.postDelayed(this::nextLine, 600);
    }

    private void fadeIn(View view, long delay) {
        view.animate().alpha(1f).setStartDelay(delay).setDuration(700).start();
    }

    private void nextLine() {
        if (line >= BOOT_LINES.length) {
            handler.postDelayed(this::openAuth, 500);
            return;
        }
        CharSequence current = bootLog.getText();
        bootLog.setText(current.length() == 0 ? BOOT_LINES[line] : current + "\n" + BOOT_LINES[line]);
        line++;

        ObjectAnimator bar = ObjectAnimator.ofInt(progress, "progress", progress.getProgress(),
                line * 100 / BOOT_LINES.length);
        bar.setDuration(LINE_DELAY_MS);
        bar.setInterpolator(new LinearInterpolator());
        bar.start();
        handler.postDelayed(this::nextLine, LINE_DELAY_MS);
    }

    private void openAuth() {
        if (finished || isFinishing()) return;
        finished = true;
        startActivity(new Intent(this, AuthActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
