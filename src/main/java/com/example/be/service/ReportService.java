package com.example.be.service;

import com.example.be.dto.EvaluationRequest;
import com.example.be.dto.EvaluationResponse;
import com.example.be.entity.Evaluation;
import com.example.be.entity.EvaluationScore;
import com.example.be.entity.InternProfile;
import com.example.be.entity.User;
import com.example.be.enums.CycleType;
import com.example.be.repository.EvaluationRepository;
import com.example.be.repository.EvaluationScoreRepository;
import com.example.be.repository.InternProfileRepository;
import com.example.be.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final EvaluationRepository evaluationRepository;
    private final EvaluationScoreRepository evaluationScoreRepository;
    private final InternProfileRepository internRepository;
    private final UserRepository userRepository;

    // Tạo mới Evaluation
    @Transactional
    public EvaluationResponse createEvaluation(EvaluationRequest req) {
        InternProfile intern = internRepository.findById(req.getInternId())
                .orElseThrow(() -> new RuntimeException("Intern not found"));
        User evaluator = userRepository.findById(req.getEvaluatorId())
                .orElseThrow(() -> new RuntimeException("Evaluator not found"));

        // ✅ Kiểm tra quyền cycleType
        if (evaluator.getRole().equals("MENTOR") &&
                (req.getCycle() != CycleType.WEEKLY && req.getCycle() != CycleType.MONTHLY)) {
            throw new RuntimeException("Mentor chỉ được tạo WEEKLY hoặc MONTHLY evaluation!");
        }
        if (evaluator.getRole().equals("HR") && req.getCycle() == CycleType.WEEKLY) {
            throw new RuntimeException("HR chỉ được tạo MONTHLY evaluation!");
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setIntern(intern);
        evaluation.setEvaluator(evaluator);
        evaluation.setCycle(req.getCycle());
        evaluation.setPeriodNo(req.getPeriodNo());
        evaluation.setComment(req.getComment());
        evaluation.setCreatedAt(LocalDateTime.now());

        evaluation = evaluationRepository.save(evaluation);

        Evaluation finalEvaluation = evaluation;
        List<EvaluationScore> scores = req.getScores().stream().map(s -> {
            EvaluationScore es = new EvaluationScore();
            es.setEvaluation(finalEvaluation);
            es.setCriteriaName(s.getCriteriaName());
            es.setScore(s.getScore());
            es.setComment(s.getComment());
            return es;
        }).collect(Collectors.toList());

        evaluationScoreRepository.saveAll(scores);
        evaluation.setScores(scores);

        return mapToResponse(evaluation);
    }

    // Cập nhật Evaluation
    @Transactional
    public EvaluationResponse updateEvaluation(Long evaluationId, EvaluationRequest req) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Evaluation not found"));

        evaluation.setComment(req.getComment());
        evaluation.setCycle(req.getCycle());
        evaluation.setPeriodNo(req.getPeriodNo());

        evaluation.getScores() .clear();

        // Thêm scores mới
        List<EvaluationScore> newScores = req.getScores().stream().map(s -> {
            EvaluationScore es = new EvaluationScore();
            es.setEvaluation(evaluation);
            es.setCriteriaName(s.getCriteriaName());
            es.setScore(s.getScore());
            es.setComment(s.getComment());
            return es;
        }).collect(Collectors.toList());

        evaluation.getScores().addAll(newScores);

        return mapToResponse(evaluationRepository.save(evaluation));
    }

    // Xóa Evaluation
    @Transactional
    public void deleteEvaluation(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Evaluation not found"));
        evaluationRepository.delete(evaluation);
    }

    // Lấy evaluation theo intern
    public List<EvaluationResponse> getEvaluationsByIntern(Long internId) {
        return evaluationRepository.findByIntern_InternId(internId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Mapping helper
    private EvaluationResponse mapToResponse(Evaluation e) {
        return EvaluationResponse.builder()
                .evaluationId(e.getEvaluationId())
                .internId(e.getIntern().getId())
                .internName(e.getIntern().getFullName())
                .evaluatorName(e.getEvaluator().getFullName())
                .cycle(e.getCycle())
                .periodNo(e.getPeriodNo())
                .comment(e.getComment())
                .createdAt(e.getCreatedAt())
                .scores(e.getScores().stream()
                        .map(s -> EvaluationResponse.ScoreResponse.builder()
                                .criteriaName(s.getCriteriaName())
                                .score(s.getScore())
                                .comment(s.getComment())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
