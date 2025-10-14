package com.example.be.repository;

import com.example.be.entity.Hr;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrRepository extends JpaRepository<Hr, Long> {
}