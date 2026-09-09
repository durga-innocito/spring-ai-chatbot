package com.innocito.spring_ai.controller;

import com.innocito.spring_ai.dto.UserResponse;
import com.innocito.spring_ai.service.ChatService;
import com.innocito.spring_ai.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/chat")
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    private String getEffectiveUserId(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null && session.getAttribute(AuthController.SESSION_USER_KEY) != null) {
            UserResponse user = (UserResponse) session.getAttribute(AuthController.SESSION_USER_KEY);
            return user.getId();
        }
        return userService.getDefaultUser().getId();
    }

    @GetMapping("/chat")
    public ResponseEntity<String> chat(
            @RequestParam(name = "conversationId", required = false, defaultValue = "default") String conversationId,
            @RequestParam(name = "message") String message,
            @RequestParam(name = "ragEnabled", required = false, defaultValue = "false") boolean ragEnabled,
            HttpServletRequest httpRequest) {
        String userId = getEffectiveUserId(httpRequest);
        String chat = chatService.chat(userId, conversationId, message, ragEnabled);
        return ResponseEntity.ok(chat);
    }
}
