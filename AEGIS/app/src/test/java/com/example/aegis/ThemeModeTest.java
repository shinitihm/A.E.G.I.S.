package com.example.aegis;

import static org.junit.Assert.assertEquals;

import androidx.appcompat.app.AppCompatDelegate;

import org.junit.Test;

public class ThemeModeTest {

    @Test
    public void starkNeverFollowsTheSystem() {
        // FOLLOW_SYSTEM faria o celular em modo escuro virar Doom sozinho
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeMode.nightModeFor(false));
    }

    @Test
    public void doomUsesTheNightResources() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeMode.nightModeFor(true));
    }
}
