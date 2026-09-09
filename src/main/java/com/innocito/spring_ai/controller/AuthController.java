package com.innocito.spring_ai.controller;

import com.innocito.spring_ai.dto.AuthRequest;
import com.innocito.spring_ai.dto.UserResponse;
import com.innocito.spring_ai.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String SESSION_USER_KEY = "LOGGED_IN_USER";

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        try {
            UserResponse user = userService.register(request);
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(SESSION_USER_KEY, user);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        try {
            UserResponse user = userService.login(request.getUsername(), request.getPassword());
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(SESSION_USER_KEY, user);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    @GetMapping("/current")
    public ResponseEntity<UserResponse> getCurrentUser(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null && session.getAttribute(SESSION_USER_KEY) != null) {
            UserResponse user = (UserResponse) session.getAttribute(SESSION_USER_KEY);
            return ResponseEntity.ok(user);
        }

        // Return or auto-create default user
        UserResponse defaultUser = userService.getDefaultUser();
        HttpSession newSession = httpRequest.getSession(true);
        newSession.setAttribute(SESSION_USER_KEY, defaultUser);
        return ResponseEntity.ok(defaultUser);
    }
}
