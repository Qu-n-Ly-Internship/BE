package com.example.be.repository;

import com.example.be.entity.InternDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<InternDocument, Long> {

    // 🔹 Lấy document mới nhất của 1 intern (dùng khi cần riêng lẻ)
    InternDocument findTopByInternProfile_IdOrderByUploadedAtDesc(Long internId);

    // 🔹 Lấy document mới nhất cho mỗi intern
    @Query(value = """
                SELECT d.*
                FROM intern_documents d
                INNER JOIN (
                    SELECT intern_id, MAX(document_id) AS max_id
                    FROM intern_documents
                    GROUP BY intern_id
                ) latest ON d.document_id = latest.max_id
            """, nativeQuery = true)
    List<InternDocument> findLatestDocumentPerIntern();
}
