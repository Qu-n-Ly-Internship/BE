package com.example.be.repository;

import com.example.be.entity.InternDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternDocumentRepository extends JpaRepository<InternDocument, Long> {

    // Lấy tài liệu theo intern_id
    List<InternDocument> findByInternProfileId(Long internId);

    // Lấy tài liệu theo trạng thái
    List<InternDocument> findByStatus(String status);

    // Lấy tài liệu theo loại tài liệu
    List<InternDocument> findByDocumentType(String documentType);

    // Lấy tài liệu theo intern và trạng thái
    List<InternDocument> findByInternProfileIdAndStatus(Long internId, String status);

    // Query tùy chỉnh để lấy tài liệu kèm thông tin intern
    @Query("SELECT d FROM InternDocument d " +
            "JOIN FETCH d.internProfile ip " +
            "LEFT JOIN FETCH ip.university " +
            "LEFT JOIN FETCH ip.program " +
            "WHERE (:status IS NULL OR d.status = :status) " +
            "AND (:documentType IS NULL OR d.documentType = :documentType)")
    List<InternDocument> findDocumentsWithFilters(
            @Param("status") String status,
            @Param("documentType") String documentType
    );

    // Đếm số tài liệu theo trạng thái
    long countByStatus(String status);

    // Lấy tài liệu chờ duyệt (PENDING)
    @Query("SELECT d FROM InternDocument d " +
            "JOIN FETCH d.internProfile ip " +
            "WHERE d.status = 'PENDING' " +
            "ORDER BY d.uploadedAt ASC")
    List<InternDocument> findPendingDocuments();
}