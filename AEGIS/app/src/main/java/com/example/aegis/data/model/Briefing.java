package com.example.aegis.data.model;

import java.util.List;

/** Resposta de GET /briefing. Cada metade pode vir vazia, com o motivo no campo *Error. */
public class Briefing {
    public String city;           // nome resolvido pelo servidor, ex.: "Campinas, São Paulo"
    public Weather weather;       // null se o clima falhou
    public String weatherError;
    public List<News> news;       // vazia se o feed falhou
    public String newsError;

    public static class Weather {
        public double temperatureC;
        public LastRain lastRain; // null = não choveu dentro da janela
        public int windowDays;    // quantos dias para trás o servidor verificou
    }

    public static class LastRain {
        public String date;       // AAAA-MM-DD
        public int daysAgo;       // 0 = hoje
        public double mm;
    }

    public static class News {
        public String title;
        public String source;
        public String url;
    }
}
