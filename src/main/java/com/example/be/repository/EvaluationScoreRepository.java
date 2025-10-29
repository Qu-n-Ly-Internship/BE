package com.example.be.repository;

import com.example.be.entity.EvaluationScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EvaluationScoreRepository extends JpaRepository<EvaluationScore, Long> {

    // ✅ Truy vấn tất cả EvaluationScore theo evaluation_id
    List<EvaluationScore> findByEvaluation_EvaluationId(Long evaluationId);
}
