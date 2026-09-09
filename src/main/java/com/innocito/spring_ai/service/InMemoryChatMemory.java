package com.innocito.spring_ai.service;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryChatMemory implements ChatMemory {

    private final Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();
    private static final int MAX_MESSAGES_PER_CONVERSATION = 20;

    @Override
    public synchronized void add(String conversationId, List<Message> messages) {
        if (conversationId == null || messages == null || messages.isEmpty()) {
            return;
        }
        List<Message> history = conversationHistory.computeIfAbsent(conversationId, k -> new ArrayList<>());
        history.addAll(messages);

        // Keep a sliding window of recent messages to preserve context without exceeding LLM context limit
        if (history.size() > MAX_MESSAGES_PER_CONVERSATION) {
            int overflow = history.size() - MAX_MESSAGES_PER_CONVERSATION;
            history.subList(0, overflow).clear();
        }
    }

    @Override
    public synchronized List<Message> get(String conversationId) {
        if (conversationId == null) {
            return List.of();
        }
        return new ArrayList<>(conversationHistory.getOrDefault(conversationId, List.of()));
    }

    @Override
    public synchronized void clear(String conversationId) {
        if (conversationId != null) {
            conversationHistory.remove(conversationId);
        }
    }
}
