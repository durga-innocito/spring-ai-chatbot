package com.innocito.spring_ai.controller;

import com.innocito.spring_ai.dto.ChatResponse;
import com.innocito.spring_ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class WebChatController {

    private final ChatService chatService;

    @GetMapping({"", "/"})
    public String index() {
        return "chat";
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<ChatResponse> sendMessage(@RequestParam String message) {
        try {
            String response = chatService.chat(message);
            return ResponseEntity.ok(new ChatResponse(message, response, true));
        } catch (Exception e) {
            return ResponseEntity.ok(new ChatResponse(message, "Error: " + e.getMessage(), false));
        }
    }
}

