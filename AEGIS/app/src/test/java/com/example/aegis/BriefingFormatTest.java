package com.example.aegis;

import static org.junit.Assert.assertEquals;

import com.example.aegis.data.model.Briefing;
import com.example.aegis.ui.briefing.BriefingFormat;

import org.junit.Test;

public class BriefingFormatTest {

    private static Briefing.Weather weather(Integer daysAgo, double mm) {
        Briefing.Weather w = new Briefing.Weather();
        w.windowDays = 30;
        if (daysAgo != null) {
            w.lastRain = new Briefing.LastRain();
            w.lastRain.daysAgo = daysAgo;
            w.lastRain.mm = mm;
        }
        return w;
    }

    @Test
    public void temperatureIsRoundedAndKeepsTheSign() {
        assertEquals("27°C", BriefingFormat.temperature(27.4));
        assertEquals("28°C", BriefingFormat.temperature(27.5));
        assertEquals("0°C", BriefingFormat.temperature(-0.4)); // nunca "-0°C"
        assertEquals("-4°C", BriefingFormat.temperature(-3.6));
    }

    @Test
    public void rainToday() {
        assertEquals("Choveu hoje (1,0 mm)", BriefingFormat.rain(weather(0, 1.0)));
    }

    @Test
    public void rainYesterday() {
        assertEquals("Choveu ontem (0,3 mm)", BriefingFormat.rain(weather(1, 0.3)));
    }

    @Test
    public void rainDaysAgoUsesDecimalComma() {
        assertEquals("Choveu há 3 dias (5,2 mm)", BriefingFormat.rain(weather(3, 5.2)));
    }

    @Test
    public void noRainInTheWindow() {
        assertEquals("Sem chuva nos últimos 30 dias", BriefingFormat.rain(weather(null, 0)));
    }
}
