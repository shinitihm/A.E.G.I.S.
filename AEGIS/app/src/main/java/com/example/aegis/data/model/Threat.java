package com.example.aegis.data.model;

import java.util.List;

/** Nível de ameaça calculado pelo backend (0–100). */
public class Threat {
    public int score;
    public String level;      // BAIXO, MODERADO, ALTO, ÔMEGA
    public String levelCode;  // LOW, MODERATE, HIGH, OMEGA
    public List<ThreatFactor> factors;

    public static class ThreatFactor {
        public String label;
        public int points;
        public int max;
    }
}
