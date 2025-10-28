package com.example.be.repository;

import com.example.be.entity.Evaluation; // Thay đổi com.yourpackage.entity.Evaluation bằng package thực tế của Entity Evaluation
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByIntern_InternId(Long internId);
}