package com.innocito.spring_ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentInfo {
    private String id;
    private String userId;
    private String fileName;
    private String fileType;
    private long fileSize;
    private int chunkCount;
    private boolean isShared;
    private LocalDateTime uploadedAt;
}
