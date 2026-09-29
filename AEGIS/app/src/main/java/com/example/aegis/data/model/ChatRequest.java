package com.example.aegis.data.model;

import java.util.List;

public class ChatRequest {
    public String message;
    public List<ChatTurn> history;

    public ChatRequest(String message, List<ChatTurn> history) {
        this.message = message;
        this.history = history;
    }
}
