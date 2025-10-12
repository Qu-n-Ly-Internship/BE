package com.example.be.repository;

import com.example.be.entity.InternDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<InternDocument, Long> {
}