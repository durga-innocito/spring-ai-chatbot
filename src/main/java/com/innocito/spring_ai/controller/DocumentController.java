package com.innocito.spring_ai.controller;

import com.innocito.spring_ai.dto.DocumentInfo;
import com.innocito.spring_ai.dto.UserResponse;
import com.innocito.spring_ai.service.DocumentService;
import com.innocito.spring_ai.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
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
    public ResponseEntity<List<DocumentInfo>> listDocuments(HttpServletRequest httpRequest) {
        String userId = getEffectiveUserId(httpRequest);
        return ResponseEntity.ok(documentService.getDocumentsForUser(userId));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isShared", defaultValue = "false") boolean isShared,
            HttpServletRequest httpRequest) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please select a non-empty file"));
            }
            String userId = getEffectiveUserId(httpRequest);
            DocumentInfo info = documentService.processAndStoreDocument(file, userId, isShared);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Failed to process uploaded document", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable String id, HttpServletRequest httpRequest) {
        try {
            String userId = getEffectiveUserId(httpRequest);
            documentService.deleteDocument(id, userId);
            return ResponseEntity.ok(Map.of("success", true, "deletedId", id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
