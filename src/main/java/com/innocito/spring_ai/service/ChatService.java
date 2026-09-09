package com.innocito.spring_ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final JpaChatMemory jpaChatMemory;
    private final DocumentService documentService;

    public ChatService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, JpaChatMemory jpaChatMemory, DocumentService documentService) {
        this.chatMemory = chatMemory;
        this.jpaChatMemory = jpaChatMemory;
        this.documentService = documentService;
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public String chat(String userId, String conversationId, String message, boolean ragEnabled) {
        String effectiveUserId = (userId != null && !userId.isBlank()) ? userId : "default_user";
        String convId = (conversationId == null || conversationId.isBlank()) ? "default" : conversationId;

        // Ensure the conversation record exists and is bound to this user
        jpaChatMemory.ensureConversation(convId, effectiveUserId, null);

        var promptSpec = chatClient.prompt().user(message);

        // If RAG (Document Q&A) is requested and the user has indexed documents
        if (ragEnabled && documentService.hasDocuments(effectiveUserId)) {
            String context = documentService.findRelevantContext(effectiveUserId, message, 4);
            if (!context.isBlank()) {
                String systemPrompt = """
                        You are a smart AI assistant. Answer the user's question using the provided document context whenever possible.
                        Cite relevant file sources when applicable. If the context does not answer the question, state so and provide your best general knowledge response.

                        --- DOCUMENT CONTEXT ---
                        """ + context + "\n-------------------------";

                promptSpec.system(systemPrompt);
            }
        }

        return promptSpec
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, convId))
                .call()
                .content();
    }

    public String chat(String conversationId, String message, boolean ragEnabled) {
        return chat("default_user", conversationId, message, ragEnabled);
    }

    public String chat(String conversationId, String message) {
        return chat("default_user", conversationId, message, false);
    }

    public String chat(String message) {
        return chat("default_user", "default", message, false);
    }

    public void clearHistory(String conversationId) {
        if (conversationId != null) {
            chatMemory.clear(conversationId);
        }
    }
}
