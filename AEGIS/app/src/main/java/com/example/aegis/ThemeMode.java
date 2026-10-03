package com.example.aegis;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Liga/desliga o tema Dr. Doom.
 *
 * O A.E.G.I.S. "empresta" o modo noturno do Android: a paleta Doom fica em res/values-night/ e o Stark nos
 * recursos normais. Por isso o Stark usa MODE_NIGHT_NO (e nunca FOLLOW_SYSTEM): senão um celular em modo
 * escuro viraria Doom sozinho.
 */
public final class ThemeMode {

    /** Cores do clarão na troca de tema (Hud.flash). São hex de propósito: é a cor do tema de DESTINO. */
    public static final int FLASH_DOOM = 0x6600E676;
    public static final int FLASH_STARK = 0x6600E5FF;

    private ThemeMode() {
    }

    public static int nightModeFor(boolean doom) {
        return doom ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
    }

    /** Aplica o tema e recria as telas abertas. */
    public static void apply(boolean doom) {
        AppCompatDelegate.setDefaultNightMode(nightModeFor(doom));
    }
}
