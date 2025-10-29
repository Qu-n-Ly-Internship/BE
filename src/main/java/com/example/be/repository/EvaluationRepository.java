package com.example.be.repository;

import com.example.be.entity.Evaluation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    // ✅ Cách đúng: truy cập vào field id của entity InternProfile
    @EntityGraph(attributePaths = {"scores"})
    List<Evaluation> findByIntern_Id(Long internId);
}
