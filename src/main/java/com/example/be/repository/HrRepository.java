package com.example.be.repository;

import com.example.be.entity.Hr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HrRepository extends JpaRepository<Hr, Long> {

    Optional<Hr> findByUser_Id(Long userId);
}