package com.example.aegis.data.model;

import java.util.List;

public class Mission {
    public int id;
    public String name;
    public String deck;
    public String imageUrl;
    public Integer issueCount;
    public String firstIssue;

    /** Retornado por /missions/{id}. */
    public static class Detail extends Mission {
        public String description;
        public List<String> issues;
    }
}
