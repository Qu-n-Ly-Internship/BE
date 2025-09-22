package com.example.be.repository;

import com.example.be.entity.InternProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InternProgressRepository extends JpaRepository<InternProgress, Long> {
}