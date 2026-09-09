package com.innocito.spring_ai.repository;

import com.innocito.spring_ai.entity.DocumentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, String> {
    List<DocumentRecord> findAllByOrderByUploadedAtDesc();

    @Query("SELECT d FROM DocumentRecord d WHERE d.userId = :userId OR d.isShared = true ORDER BY d.uploadedAt DESC")
    List<DocumentRecord> findAccessibleDocuments(@Param("userId") String userId);

    Optional<DocumentRecord> findByIdAndUserId(String id, String userId);

    long countByUserId(String userId);

    void deleteByIdAndUserId(String id, String userId);
}
