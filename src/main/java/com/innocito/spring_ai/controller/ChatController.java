package com.innocito.spring_ai.controller;

import com.innocito.spring_ai.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/chat")
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam(name = "message") String message) {
        String chat = chatService.chat(message);
        return ResponseEntity.ok(chat);
    }
}
