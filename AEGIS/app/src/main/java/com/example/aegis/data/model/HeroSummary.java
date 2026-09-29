package com.example.aegis.data.model;

public class HeroSummary {
    public int id;
    public String name;
    public String realName;
    public String deck;
    public String imageUrl;
    public String thumbUrl;
    public int appearances;
    public String firstAppearance;
    public String publisher;
    public Threat threat; // null enquanto o alvo não foi escaneado
}
