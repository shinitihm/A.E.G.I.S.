package com.example.aegis.ui.briefing;

import com.example.aegis.data.model.Briefing;

import java.util.Locale;

/** Textos do briefing. Não depende do Android, para poder ser testado na JVM. */
public final class BriefingFormat {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private BriefingFormat() {
    }

    /** 27.4 -> "27°C". */
    public static String temperature(double celsius) {
        return Math.round(celsius) + "°C";
    }

    /** "Choveu ontem (5,2 mm)", "Choveu há 3 dias (5,2 mm)" ou "Sem chuva nos últimos 30 dias". */
    public static String rain(Briefing.Weather weather) {
        Briefing.LastRain last = weather.lastRain;
        if (last == null) return "Sem chuva nos últimos " + weather.windowDays + " dias";
        String when = last.daysAgo == 0 ? "hoje" : last.daysAgo == 1 ? "ontem" : "há " + last.daysAgo + " dias";
        return "Choveu " + when + " (" + String.format(PT_BR, "%.1f", last.mm) + " mm)";
    }
}
