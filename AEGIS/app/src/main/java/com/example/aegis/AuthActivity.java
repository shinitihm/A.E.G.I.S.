package com.example.aegis;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.example.aegis.ui.hud.Hud;
import com.example.aegis.ui.hud.ScanRingView;

/**
 * Autenticação por digital. Usa o BiometricPrompt do Android (sensor real) com o credencial
 * do aparelho (PIN/padrão) como "acesso de emergência" caso não haja digital cadastrada.
 */
public class AuthActivity extends AppCompatActivity {

    private static final int BIOMETRIC = BiometricManager.Authenticators.BIOMETRIC_STRONG;
    // Só o PIN/padrão do aparelho: a combinação BIOMETRIC_STRONG | DEVICE_CREDENTIAL não funciona no Android 9 e 10.
    private static final int DEVICE_PIN = BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ScanRingView ring;
    private ImageView fingerprint;
    private View scanLine;
    private TextView status;
    private View retry;
    private View emergency;
    private View flash;
    private ObjectAnimator scanAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Hud.setContent(this, R.layout.activity_auth);

        ring = findViewById(R.id.ring);
        fingerprint = findViewById(R.id.fingerprint);
        scanLine = findViewById(R.id.scanLine);
        status = findViewById(R.id.status);
        retry = findViewById(R.id.retry);
        emergency = findViewById(R.id.emergency);
        flash = findViewById(R.id.flash);

        View.OnClickListener scan = v -> startScan(false);
        findViewById(R.id.scanner).setOnClickListener(scan);
        retry.setOnClickListener(scan);
        emergency.setOnClickListener(v -> startScan(true));

        setIdle(getString(R.string.auth_idle), ContextCompat.getColor(this, R.color.aegis_cyan));
        // dá um instante para o usuário ver a tela antes de abrir o sensor
        handler.postDelayed(() -> startScan(false), 700);
    }

    private void startScan(boolean useDeviceCredential) {
        if (isFinishing()) return;
        int authenticators = useDeviceCredential ? DEVICE_PIN : BIOMETRIC;

        int can = BiometricManager.from(this).canAuthenticate(authenticators);
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            setIdle(getString(R.string.auth_unavailable), ContextCompat.getColor(this, R.color.aegis_gold));
            emergency.setVisibility(View.VISIBLE);
            return;
        }

        setScanning();
        BiometricPrompt.PromptInfo.Builder info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.auth_prompt_title))
                .setSubtitle(getString(R.string.auth_prompt_subtitle))
                .setAllowedAuthenticators(authenticators);
        if (!useDeviceCredential) info.setNegativeButtonText(getString(android.R.string.cancel));

        new BiometricPrompt(this, ContextCompat.getMainExecutor(this), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                granted();
            }

            @Override
            public void onAuthenticationFailed() {
                // digital lida mas não reconhecida — o prompt continua aberto para nova tentativa
                Hud.vibrate(AuthActivity.this, 60);
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                boolean cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED
                        || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        || errorCode == BiometricPrompt.ERROR_CANCELED;
                denied(cancelled ? getString(R.string.auth_cancelled) : getString(R.string.auth_denied), !cancelled);
            }
        }).authenticate(info.build());
    }

    private void setIdle(String message, int color) {
        stopScanLine();
        ring.setScanning(false);
        ring.setColor(color);
        fingerprint.setColorFilter(color);
        status.setText(message);
        status.setTextColor(color);
        retry.setVisibility(View.VISIBLE);
    }

    private void setScanning() {
        int cyan = ContextCompat.getColor(this, R.color.aegis_cyan);
        ring.setColor(cyan);
        ring.setScanning(true);
        fingerprint.setColorFilter(cyan);
        status.setText(R.string.auth_scanning);
        status.setTextColor(cyan);
        retry.setVisibility(View.INVISIBLE);
        startScanLine();
    }

    private void startScanLine() {
        scanLine.setVisibility(View.VISIBLE);
        if (scanAnimator != null) scanAnimator.cancel();
        float span = Hud.dp(this, 150);
        scanLine.setTranslationY(-span / 2f);
        scanAnimator = ObjectAnimator.ofFloat(scanLine, View.TRANSLATION_Y, -span / 2f, span / 2f);
        scanAnimator.setDuration(1100);
        scanAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        scanAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        scanAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scanAnimator.start();
    }

    private void stopScanLine() {
        if (scanAnimator != null) scanAnimator.cancel();
        scanLine.setVisibility(View.INVISIBLE);
    }

    private void denied(String message, boolean alarm) {
        int red = ContextCompat.getColor(this, alarm ? R.color.aegis_red : R.color.aegis_gold);
        setIdle(message, red);
        emergency.setVisibility(View.VISIBLE);
        if (alarm) {
            Hud.vibrate(this, 300);
            Hud.flash(flash, 0x55FF1744);
        }
    }

    private void granted() {
        stopScanLine();
        int cyan = ContextCompat.getColor(this, R.color.aegis_cyan);
        ring.setColor(cyan);
        fingerprint.setColorFilter(Color.WHITE);
        retry.setVisibility(View.INVISIBLE);
        emergency.setVisibility(View.GONE);
        Hud.vibrate(this, 120);
        Hud.typewriter(status, getString(R.string.auth_success));
        Hud.flash(flash, ColorUtils.setAlphaComponent(cyan, 0x33));
        handler.postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1700);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (scanAnimator != null) scanAnimator.cancel();
        super.onDestroy();
    }
}
