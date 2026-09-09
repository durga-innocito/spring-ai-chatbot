package com.innocito.spring_ai.service;

import com.innocito.spring_ai.dto.DocumentInfo;
import com.innocito.spring_ai.entity.DocumentChunk;
import com.innocito.spring_ai.entity.DocumentRecord;
import com.innocito.spring_ai.repository.DocumentChunkRepository;
import com.innocito.spring_ai.repository.DocumentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRecordRepository documentRecordRepository;
    private final DocumentChunkRepository documentChunkRepository;

    private static final int CHUNK_SIZE_CHARS = 1000;
    private static final int CHUNK_OVERLAP_CHARS = 150;

    @Transactional
    public DocumentInfo processAndStoreDocument(MultipartFile file, String userId, boolean isShared) throws Exception {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        String effectiveUserId = (userId != null && !userId.isBlank()) ? userId : "default_user";
        String documentId = "doc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String textContent = extractText(file);

        if (textContent.isBlank()) {
            throw new IllegalArgumentException("Could not extract any readable text from " + originalFilename);
        }

        List<String> textChunks = splitIntoChunks(textContent);

        List<DocumentChunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < textChunks.size(); i++) {
            chunkEntities.add(DocumentChunk.builder()
                    .documentId(documentId)
                    .fileName(originalFilename)
                    .chunkIndex(i)
                    .text(textChunks.get(i))
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        documentChunkRepository.saveAll(chunkEntities);

        DocumentRecord record = DocumentRecord.builder()
                .id(documentId)
                .userId(effectiveUserId)
                .fileName(originalFilename)
                .fileType(getFileExtension(originalFilename))
                .fileSize(file.getSize())
                .chunkCount(chunkEntities.size())
                .isShared(isShared)
                .uploadedAt(LocalDateTime.now())
                .build();
        documentRecordRepository.save(record);

        log.info("Indexed document '{}' for user '{}' with {} chunks (id: {})", originalFilename, effectiveUserId, chunkEntities.size(), documentId);

        return DocumentInfo.builder()
                .id(documentId)
                .userId(effectiveUserId)
                .fileName(originalFilename)
                .fileType(record.getFileType())
                .fileSize(record.getFileSize())
                .chunkCount(record.getChunkCount())
                .isShared(record.isShared())
                .uploadedAt(record.getUploadedAt())
                .build();
    }

    public List<DocumentInfo> getDocumentsForUser(String userId) {
        String effectiveUserId = (userId != null && !userId.isBlank()) ? userId : "default_user";
        return documentRecordRepository.findAccessibleDocuments(effectiveUserId).stream()
                .map(rec -> DocumentInfo.builder()
                        .id(rec.getId())
                        .userId(rec.getUserId())
                        .fileName(rec.getFileName())
                        .fileType(rec.getFileType())
                        .fileSize(rec.getFileSize())
                        .chunkCount(rec.getChunkCount())
                        .isShared(rec.isShared())
                        .uploadedAt(rec.getUploadedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public List<DocumentInfo> getAllDocuments() {
        return getDocumentsForUser("default_user");
    }

    @Transactional
    public void deleteDocument(String documentId, String userId) {
        String effectiveUserId = (userId != null && !userId.isBlank()) ? userId : "default_user";
        documentRecordRepository.findByIdAndUserId(documentId, effectiveUserId).ifPresentOrElse(rec -> {
            documentChunkRepository.deleteByDocumentId(documentId);
            documentRecordRepository.delete(rec);
            log.info("Deleted document '{}' owned by user '{}'", documentId, effectiveUserId);
        }, () -> {
            throw new IllegalArgumentException("Document not found or you do not have permission to delete it.");
        });
    }

    public boolean hasDocuments(String userId) {
        String effectiveUserId = (userId != null && !userId.isBlank()) ? userId : "default_user";
        return !documentRecordRepository.findAccessibleDocuments(effectiveUserId).isEmpty();
    }

    /**
     * Retrieves the most relevant document chunks based on semantic term scoring,
     * strictly isolated to documents accessible to the specific user.
     */
    public String findRelevantContext(String userId, String query, int topK) {
        if (query == null || query.isBlank()) {
            return "";
        }

        String effectiveUserId = (userId != null && !userId.isBlank()) ? userId : "default_user";
        List<DocumentRecord> accessibleDocs = documentRecordRepository.findAccessibleDocuments(effectiveUserId);
        if (accessibleDocs.isEmpty()) {
            return "";
        }

        Set<String> accessibleDocIds = accessibleDocs.stream().map(DocumentRecord::getId).collect(Collectors.toSet());

        List<DocumentChunk> allChunks = documentChunkRepository.findAll().stream()
                .filter(chunk -> accessibleDocIds.contains(chunk.getDocumentId()))
                .collect(Collectors.toList());

        if (allChunks.isEmpty()) {
            return "";
        }

        Set<String> queryWords = Arrays.stream(query.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 2)
                .collect(Collectors.toSet());

        if (queryWords.isEmpty()) {
            return allChunks.stream().limit(topK).map(DocumentChunk::getText).collect(Collectors.joining("\n\n---\n\n"));
        }

        // Score chunks by term frequency & relevance
        List<ScoredChunk> scored = new ArrayList<>();
        for (DocumentChunk chunk : allChunks) {
            String lowerText = chunk.getText().toLowerCase();
            int score = 0;
            for (String word : queryWords) {
                if (lowerText.contains(word)) {
                    score += 10;
                    int index = 0;
                    while ((index = lowerText.indexOf(word, index)) != -1) {
                        score += 2;
                        index += word.length();
                    }
                }
            }
            if (score > 0) {
                scored.add(new ScoredChunk(chunk, score));
            }
        }

        scored.sort((a, b) -> Integer.compare(b.score, a.score));

        return scored.stream()
                .limit(topK)
                .map(sc -> "[From: " + sc.chunk.getFileName() + "]\n" + sc.chunk.getText())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String extractText(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (filename.endsWith(".pdf")) {
            try (InputStream is = file.getInputStream();
                 PDDocument document = Loader.loadPDF(is.readAllBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } else {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
    }

    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = Math.min(start + CHUNK_SIZE_CHARS, length);
            if (end < length) {
                int lastBreak = text.lastIndexOf("\n\n", end);
                if (lastBreak > start + 300) {
                    end = lastBreak;
                } else {
                    int lastPeriod = text.lastIndexOf(". ", end);
                    if (lastPeriod > start + 300) {
                        end = lastPeriod + 1;
                    }
                }
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            start = Math.max(start + 1, end - CHUNK_OVERLAP_CHARS);
        }
        return chunks;
    }

    private String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot != -1 ? filename.substring(dot + 1).toUpperCase() : "FILE";
    }

    private static class ScoredChunk {
        DocumentChunk chunk;
        int score;

        ScoredChunk(DocumentChunk chunk, int score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
