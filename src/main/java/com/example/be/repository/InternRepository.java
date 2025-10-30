package com.example.be.repository;

import com.example.be.entity.InternProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface InternRepository extends JpaRepository<InternProfile, Long> {

    @Query(value = """
        SELECT * FROM intern_profiles i
        WHERE (:query IS NULL OR 
              LOWER(i.full_name) LIKE LOWER(CONCAT('%', :query, '%')) OR
              LOWER(i.email) LIKE LOWER(CONCAT('%', :query, '%')))
        AND (:university IS NULL OR LOWER(i.university) = LOWER(:university))
        AND (:major IS NULL OR LOWER(i.major) = LOWER(:major))
        AND (:program IS NULL OR LOWER(i.program) = LOWER(:program))
        AND (:yearOfStudy IS NULL OR i.year_of_study = :yearOfStudy)
        AND (:status IS NULL OR UPPER(i.status) = UPPER(:status))
        ORDER BY i.updated_at DESC
    """,
    countQuery = """
        SELECT COUNT(*) FROM intern_profiles i
        WHERE (:query IS NULL OR 
              LOWER(i.full_name) LIKE LOWER(CONCAT('%', :query, '%')) OR
              LOWER(i.email) LIKE LOWER(CONCAT('%', :query, '%')))
        AND (:university IS NULL OR LOWER(i.university) = LOWER(:university))
        AND (:major IS NULL OR LOWER(i.major) = LOWER(:major))
        AND (:program IS NULL OR LOWER(i.program) = LOWER(:program))
        AND (:yearOfStudy IS NULL OR i.year_of_study = :yearOfStudy)
        AND (:status IS NULL OR UPPER(i.status) = UPPER(:status))
    """,
    nativeQuery = true)
    Page<Map<String, Object>> searchInterns(
        @Param("query") String query,
        @Param("university") String university,
        @Param("major") String major,
        @Param("program") String program,
        @Param("yearOfStudy") Integer yearOfStudy,
        @Param("status") String status,
        Pageable pageable
    );

    @Query(value = "SELECT DISTINCT university FROM intern_profiles WHERE university IS NOT NULL ORDER BY university",
           nativeQuery = true)
    List<String> findDistinctUniversities();

    @Query(value = "SELECT DISTINCT major FROM intern_profiles WHERE major IS NOT NULL ORDER BY major",
           nativeQuery = true)
    List<String> findDistinctMajors();

    @Query(value = "SELECT status, COUNT(*) FROM intern_profiles GROUP BY status",
           nativeQuery = true)
    List<Object[]> countByStatus();

    @Query(value = "SELECT university, COUNT(*) FROM intern_profiles WHERE university IS NOT NULL GROUP BY university",
           nativeQuery = true)
    List<Object[]> countByUniversity();

    @Query(value = "SELECT program, COUNT(*) FROM intern_profiles WHERE program IS NOT NULL GROUP BY program",
           nativeQuery = true)
    List<Object[]> countByProgram();

    Optional<InternProfile> findByUser_Id(Long userId);
}
