package com.example.aegis.data.model;

public class ChatTurn {
    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";

    public String role;
    public String text;

    public ChatTurn(String role, String text) {
        this.role = role;
        this.text = text;
    }

    public boolean isUser() {
        return USER.equals(role);
    }
}
