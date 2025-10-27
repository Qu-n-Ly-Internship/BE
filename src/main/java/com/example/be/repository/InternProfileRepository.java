package com.example.be.repository;

import com.example.be.entity.InternProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InternProfileRepository extends JpaRepository<InternProfile, Long> {

    Optional<InternProfile> findByUser_Id(Long internId);

    List<InternProfile> findByProgram_Id(Long programId);
}
