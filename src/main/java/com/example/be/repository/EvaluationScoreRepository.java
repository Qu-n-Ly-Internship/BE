package com.example.be.repository;


import com.example.be.entity.EvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationScoreRepository extends JpaRepository<EvaluationScore, Long> {
   }