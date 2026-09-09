package com.innocito.spring_ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ChatService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public String chat(String conversationId, String message) {
        String convId = (conversationId == null || conversationId.isBlank()) ? "default" : conversationId;
        return chatClient.prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, convId))
                .call()
                .content();
    }

    public String chat(String message) {
        return chat("default", message);
    }

    public void clearHistory(String conversationId) {
        if (conversationId != null) {
            chatMemory.clear(conversationId);
        }
    }
}


