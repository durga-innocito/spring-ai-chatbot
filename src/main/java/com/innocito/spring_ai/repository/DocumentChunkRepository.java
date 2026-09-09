package com.innocito.spring_ai.repository;

import com.innocito.spring_ai.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentId(String documentId);
    void deleteByDocumentId(String documentId);
}
