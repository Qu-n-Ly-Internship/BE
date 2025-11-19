package com.example.be.repository;

import com.example.be.entity.InternProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InternProfileRepository extends JpaRepository<InternProfile, Long> {

    Optional<InternProfile> findByUser_Id(Long internId);

    List<InternProfile> findByProgram_Id(Long programId);

    @Query("""
                SELECT ip FROM InternProfile ip
                JOIN ip.program p
                WHERE p.mentor.id = :mentorId
                  AND ip.status = 'active'
            """)
    List<InternProfile> findActiveInternsByMentorId(@Param("mentorId") Long mentorId);

}
