package com.example.be.repository;

import com.example.be.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByInternId(Long internId);
}
