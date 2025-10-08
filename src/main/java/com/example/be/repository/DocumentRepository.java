package com.example.be.repository;

import com.example.be.entity.InternDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DocumentRepository extends JpaRepository<InternDocument, Long> {

    @Query("SELECT d.fileDetail FROM InternDocument d WHERE d.internProfile.id = :internId")
    List<String> findFileUrlsByInternId(@Param("internId") Long internId);
}
