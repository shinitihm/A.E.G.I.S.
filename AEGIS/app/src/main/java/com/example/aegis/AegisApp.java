package com.example.aegis;

import android.app.Application;

import com.example.aegis.data.LocalStore;

/** Reaplica o tema salvo (Stark ou Doom) sempre que o processo do app nasce, antes de qualquer tela. */
public class AegisApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeMode.apply(new LocalStore(this).doomMode());
    }
}
