package com.example.aegis.ui.hud;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

/** A voz da J.A.R.V.I.S. (TextToSpeech em português). */
public class Speaker implements TextToSpeech.OnInitListener {

    public interface Listener {
        void onSpeaking(boolean speaking);
    }

    private final TextToSpeech tts;
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean ready;
    private String pending;
    private Listener listener;

    public Speaker(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), this);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) return;
        tts.setLanguage(Locale.forLanguageTag("pt-BR"));
        tts.setPitch(0.85f);      // um pouco mais grave
        tts.setSpeechRate(1.05f);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String id) {
                notifySpeaking(true);
            }

            @Override
            public void onDone(String id) {
                notifySpeaking(false);
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onError(String id) {
                notifySpeaking(false);
            }
        });
        ready = true;
        if (pending != null) {
            speak(pending);
            pending = null;
        }
    }

    public void speak(String text) {
        if (!ready) {
            pending = text;
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aegis-" + System.nanoTime());
    }

    public void stop() {
        pending = null;
        if (ready) tts.stop();
        notifySpeaking(false);
    }

    public void shutdown() {
        listener = null;
        tts.stop();
        tts.shutdown();
    }

    private void notifySpeaking(boolean speaking) {
        main.post(() -> {
            if (listener != null) listener.onSpeaking(speaking);
        });
    }
}
