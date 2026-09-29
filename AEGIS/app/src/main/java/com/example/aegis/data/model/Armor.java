package com.example.aegis.data.model;

import java.util.List;

public class Armor {
    public int mark;
    public String code;
    public String name;
    public String nickname;
    public int year;
    public String debut;
    public String armorClass;
    public String status;
    public String description;
    public List<String> weapons;
    public Stats stats;
    public String primaryColor;
    public String secondaryColor;
    public String imageUrl;

    public static class Stats {
        public int power;
        public int armor;
        public int speed;
        public int flight;
        public int tech;
    }
}
