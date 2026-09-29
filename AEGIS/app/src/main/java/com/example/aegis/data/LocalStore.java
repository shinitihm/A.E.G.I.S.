package com.example.aegis.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.aegis.data.model.ChatTurn;
import com.example.aegis.data.model.HeroSummary;
import com.example.aegis.data.model.Threat;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dados salvos no aparelho: alvos monitorados, ameaças já escaneadas e histórico do chat. */
public class LocalStore {

    private static final String MONITORED = "monitored";
    private static final String THREATS = "threats";
    private static final String CHAT = "chat";
    private static final String VOICE = "voice_enabled";
    private static final int CHAT_LIMIT = 60;

    private final SharedPreferences prefs;

    public LocalStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("aegis", Context.MODE_PRIVATE);
    }

    // ── alvos monitorados ──

    public List<HeroSummary> monitored() {
        return read(MONITORED, new TypeToken<List<HeroSummary>>() { }.getType(), new ArrayList<>());
    }

    public boolean isMonitored(int id) {
        for (HeroSummary h : monitored()) {
            if (h.id == id) return true;
        }
        return false;
    }

    /** Liga/desliga o monitoramento. Retorna o novo estado. */
    public boolean toggleMonitored(HeroSummary hero) {
        List<HeroSummary> list = monitored();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == hero.id) {
                list.remove(i);
                write(MONITORED, list);
                return false;
            }
        }
        HeroSummary copy = new HeroSummary(); // guarda só o resumo, não a ficha inteira
        copy.id = hero.id;
        copy.name = hero.name;
        copy.realName = hero.realName;
        copy.thumbUrl = hero.thumbUrl;
        copy.imageUrl = hero.imageUrl;
        copy.appearances = hero.appearances;
        copy.threat = hero.threat;
        list.add(0, copy);
        write(MONITORED, list);
        return true;
    }

    // ── ameaças escaneadas ──

    public Map<Integer, Threat> threats() {
        return read(THREATS, new TypeToken<Map<Integer, Threat>>() { }.getType(), new HashMap<>());
    }

    public void saveThreat(int heroId, Threat threat) {
        Map<Integer, Threat> map = threats();
        map.put(heroId, threat);
        write(THREATS, map);
    }

    // ── chat da J.A.R.V.I.S. ──

    public List<ChatTurn> chat() {
        return read(CHAT, new TypeToken<List<ChatTurn>>() { }.getType(), new ArrayList<>());
    }

    public void saveChat(List<ChatTurn> turns) {
        int from = Math.max(0, turns.size() - CHAT_LIMIT);
        write(CHAT, new ArrayList<>(turns.subList(from, turns.size())));
    }

    public boolean voiceEnabled() {
        return prefs.getBoolean(VOICE, true);
    }

    public void setVoiceEnabled(boolean enabled) {
        prefs.edit().putBoolean(VOICE, enabled).apply();
    }

    // ── helpers ──

    private <T> T read(String key, Type type, T fallback) {
        String json = prefs.getString(key, null);
        if (json == null) return fallback;
        try {
            T value = ApiClient.GSON.fromJson(json, type);
            return value != null ? value : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private void write(String key, Object value) {
        prefs.edit().putString(key, ApiClient.GSON.toJson(value)).apply();
    }
}
