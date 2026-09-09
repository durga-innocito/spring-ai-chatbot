package com.innocito.spring_ai.controller;

import com.innocito.spring_ai.dto.UserResponse;
import com.innocito.spring_ai.entity.ChatConversation;
import com.innocito.spring_ai.entity.ChatMessage;
import com.innocito.spring_ai.repository.ChatConversationRepository;
import com.innocito.spring_ai.repository.ChatMessageRepository;
import com.innocito.spring_ai.service.JpaChatMemory;
import com.innocito.spring_ai.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final JpaChatMemory chatMemory;
    private final UserService userService;

    private String getEffectiveUserId(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null && session.getAttribute(AuthController.SESSION_USER_KEY) != null) {
            UserResponse user = (UserResponse) session.getAttribute(AuthController.SESSION_USER_KEY);
            return user.getId();
        }
        return userService.getDefaultUser().getId();
    }

    @GetMapping
    public ResponseEntity<List<ChatConversation>> getAllConversations(HttpServletRequest httpRequest) {
        String userId = getEffectiveUserId(httpRequest);
        return ResponseEntity.ok(conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId));
    }

    @PostMapping
    public ResponseEntity<ChatConversation> createConversation(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest httpRequest) {

        String userId = getEffectiveUserId(httpRequest);
        String id = (body != null && body.containsKey("id") && !body.get("id").isBlank())
                ? body.get("id")
                : "conv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        String title = (body != null && body.containsKey("title") && !body.get("title").isBlank())
                ? body.get("title")
                : "New Conversation";

        ChatConversation conversation = ChatConversation.builder()
                .id(id)
                .userId(userId)
                .title(title)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(conversationRepository.save(conversation));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable String id) {
        return ResponseEntity.ok(messageRepository.findByConversationIdOrderByCreatedAtAsc(id));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteConversation(
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        String userId = getEffectiveUserId(httpRequest);
        chatMemory.clearForUser(id, userId);
        return ResponseEntity.ok(Map.of("success", true, "deletedId", id));
    }

    @PutMapping("/{id}/title")
    public ResponseEntity<ChatConversation> updateTitle(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {
        String userId = getEffectiveUserId(httpRequest);
        return conversationRepository.findByIdAndUserId(id, userId).map(conv -> {
            if (body != null && body.containsKey("title")) {
                conv.setTitle(body.get("title"));
                conv.setUpdatedAt(LocalDateTime.now());
                conversationRepository.save(conv);
            }
            return ResponseEntity.ok(conv);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
