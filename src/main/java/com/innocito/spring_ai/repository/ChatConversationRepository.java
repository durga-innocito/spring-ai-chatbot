package com.innocito.spring_ai.repository;

import com.innocito.spring_ai.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {
    List<ChatConversation> findAllByOrderByUpdatedAtDesc();
}
