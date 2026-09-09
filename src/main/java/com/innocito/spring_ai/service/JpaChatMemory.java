package com.innocito.spring_ai.service;

import com.innocito.spring_ai.entity.ChatConversation;
import com.innocito.spring_ai.entity.ChatMessage;
import com.innocito.spring_ai.repository.ChatConversationRepository;
import com.innocito.spring_ai.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class JpaChatMemory implements ChatMemory {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;

    private static final int MAX_CONTEXT_MESSAGES = 20;

    @Override
    @Transactional
    public void add(String conversationId, List<Message> messages) {
        if (conversationId == null || messages == null || messages.isEmpty()) {
            return;
        }

        // 1. Ensure conversation session exists
        ChatConversation conversation = conversationRepository.findById(conversationId).orElseGet(() -> {
            String initialTitle = "New Conversation";
            // If the first message is from a user, generate a title preview
            for (Message msg : messages) {
                if (msg.getMessageType() == MessageType.USER) {
                    initialTitle = generateTitleFromMessage(msg.getText());
                    break;
                }
            }
            ChatConversation newConv = ChatConversation.builder()
                    .id(conversationId)
                    .title(initialTitle)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return conversationRepository.save(newConv);
        });

        // If title is default, update with first user message
        if ("New Conversation".equals(conversation.getTitle())) {
            for (Message msg : messages) {
                if (msg.getMessageType() == MessageType.USER) {
                    conversation.setTitle(generateTitleFromMessage(msg.getText()));
                    break;
                }
            }
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // 2. Persist new messages
        List<ChatMessage> entities = new ArrayList<>();
        for (Message msg : messages) {
            String typeStr = msg.getMessageType() != null ? msg.getMessageType().name() : "USER";
            ChatMessage entity = ChatMessage.builder()
                    .conversationId(conversationId)
                    .messageType(typeStr)
                    .content(msg.getText())
                    .createdAt(LocalDateTime.now())
                    .build();
            entities.add(entity);
        }
        messageRepository.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> get(String conversationId) {
        if (conversationId == null) {
            return List.of();
        }

        List<ChatMessage> dbMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (dbMessages.isEmpty()) {
            return List.of();
        }

        // Apply sliding window for LLM context limit
        int startIdx = Math.max(0, dbMessages.size() - MAX_CONTEXT_MESSAGES);
        List<ChatMessage> window = dbMessages.subList(startIdx, dbMessages.size());

        List<Message> result = new ArrayList<>();
        for (ChatMessage msg : window) {
            if ("USER".equalsIgnoreCase(msg.getMessageType())) {
                result.add(new UserMessage(msg.getContent()));
            } else if ("ASSISTANT".equalsIgnoreCase(msg.getMessageType())) {
                result.add(new AssistantMessage(msg.getContent()));
            } else if ("SYSTEM".equalsIgnoreCase(msg.getMessageType())) {
                result.add(new SystemMessage(msg.getContent()));
            } else {
                result.add(new UserMessage(msg.getContent()));
            }
        }
        return result;
    }

    @Override
    @Transactional
    public void clear(String conversationId) {
        if (conversationId != null) {
            messageRepository.deleteByConversationId(conversationId);
            conversationRepository.deleteById(conversationId);
        }
    }

    private String generateTitleFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return "New Conversation";
        }
        String cleaned = message.trim().replaceAll("[\\r\\n]+", " ");
        if (cleaned.length() > 40) {
            return cleaned.substring(0, 37) + "...";
        }
        return cleaned;
    }
}
