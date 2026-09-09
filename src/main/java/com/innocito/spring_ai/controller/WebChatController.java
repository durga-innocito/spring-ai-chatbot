package com.innocito.spring_ai.controller;

import com.innocito.spring_ai.dto.ChatResponse;
import com.innocito.spring_ai.dto.UserResponse;
import com.innocito.spring_ai.service.ChatService;
import com.innocito.spring_ai.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class WebChatController {

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

    @GetMapping({"", "/"})
    public String index() {
        return "chat";
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<ChatResponse> sendMessage(
            @RequestParam(required = false, defaultValue = "default") String conversationId,
            @RequestParam String message,
            @RequestParam(required = false, defaultValue = "false") boolean ragEnabled,
            HttpServletRequest httpRequest) {
        try {
            String userId = getEffectiveUserId(httpRequest);
            String response = chatService.chat(userId, conversationId, message, ragEnabled);
            return ResponseEntity.ok(new ChatResponse(message, response, true));
        } catch (Exception e) {
            return ResponseEntity.ok(new ChatResponse(message, "Error: " + e.getMessage(), false));
        }
    }
}
