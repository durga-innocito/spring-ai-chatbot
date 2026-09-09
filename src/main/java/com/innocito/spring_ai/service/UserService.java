package com.innocito.spring_ai.service;

import com.innocito.spring_ai.dto.AuthRequest;
import com.innocito.spring_ai.dto.UserResponse;
import com.innocito.spring_ai.entity.AppUser;
import com.innocito.spring_ai.repository.AppUserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;

    @PostConstruct
    public void initDefaultUsers() {
        if (userRepository.count() == 0) {
            log.info("Seeding initial default users for multi-user system...");
            registerInternal("user", "password123", "Demo User", "user@example.com", "ROLE_USER");
            registerInternal("admin", "admin123", "Administrator", "admin@example.com", "ROLE_ADMIN");
            log.info("Default users seeded: [user, admin]");
        }
    }

    @Transactional
    public UserResponse register(AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long");
        }
        if (request.getPassword() == null || request.getPassword().trim().length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters long");
        }

        String username = request.getUsername().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken");
        }

        String fullName = (request.getFullName() != null && !request.getFullName().isBlank())
                ? request.getFullName().trim()
                : username;

        AppUser user = registerInternal(username, request.getPassword(), fullName, request.getEmail(), "ROLE_USER");
        return toUserResponse(user);
    }

    public UserResponse login(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username and password are required");
        }

        String normalizedUsername = username.trim().toLowerCase();
        AppUser user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        String computedHash = hashPassword(password, user.getSalt());
        if (!computedHash.equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return toUserResponse(user);
    }

    public Optional<UserResponse> findById(String userId) {
        return userRepository.findById(userId).map(this::toUserResponse);
    }

    public Optional<UserResponse> findByUsername(String username) {
        return userRepository.findByUsername(username.trim().toLowerCase()).map(this::toUserResponse);
    }

    public UserResponse getDefaultUser() {
        return userRepository.findByUsername("user")
                .map(this::toUserResponse)
                .orElseGet(() -> {
                    AppUser user = registerInternal("user", "password123", "Demo User", "user@example.com", "ROLE_USER");
                    return toUserResponse(user);
                });
    }

    private AppUser registerInternal(String username, String rawPassword, String fullName, String email, String role) {
        String salt = generateSalt();
        String hash = hashPassword(rawPassword, salt);
        String userId = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        AppUser user = AppUser.builder()
                .id(userId)
                .username(username)
                .fullName(fullName)
                .email(email != null ? email.trim() : null)
                .salt(salt)
                .passwordHash(hash)
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(salt));
            byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    public UserResponse toUserResponse(AppUser user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
