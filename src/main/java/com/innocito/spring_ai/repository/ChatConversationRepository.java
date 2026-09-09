package com.innocito.spring_ai.repository;

import com.innocito.spring_ai.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {
    List<ChatConversation> findAllByOrderByUpdatedAtDesc();
    List<ChatConversation> findByUserIdOrderByUpdatedAtDesc(String userId);
    Optional<ChatConversation> findByIdAndUserId(String id, String userId);
    void deleteByIdAndUserId(String id, String userId);
}
