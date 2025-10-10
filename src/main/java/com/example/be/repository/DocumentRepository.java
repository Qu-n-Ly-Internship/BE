package com.example.be.repository;

import com.example.be.entity.InternDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DocumentRepository extends JpaRepository<InternDocument, Long> {

    // Lấy document mới nhất theo internId (uploadedAt mới nhất)
    InternDocument findTopByInternProfile_IdOrderByUploadedAtDesc(Long internId);
}
