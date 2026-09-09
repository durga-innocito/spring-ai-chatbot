package com.innocito.spring_ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_records", indexes = {
    @Index(name = "idx_doc_record_user_id", columnList = "userId"),
    @Index(name = "idx_doc_record_uploaded_at", columnList = "uploadedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentRecord {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    @Builder.Default
    private String userId = "default_user";

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 50)
    private String fileType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private int chunkCount;

    @Column(nullable = false)
    @Builder.Default
    private boolean isShared = false;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        if (userId == null || userId.isBlank()) {
            userId = "default_user";
        }
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}
