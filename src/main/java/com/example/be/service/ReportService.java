package com.example.be.service;

import com.example.be.dto.EvaluationRequest;
import com.example.be.dto.EvaluationResponse;
import com.example.be.dto.EvaluationScoreResponse;
import com.example.be.entity.*;
import com.example.be.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
public class ReportService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationScoreRepository evaluationScoreRepository;
    private final MentorContextService mentorContextService;
    private final HrContextService hrContextService;
    private final InternRepository internRepository;
    private final MentorRepository mentorRepository;
    private final HrRepository hrRepository;

    public ReportService(EvaluationRepository evaluationRepository,
                         EvaluationScoreRepository evaluationScoreRepository,
                         MentorContextService mentorContextService,
                         HrContextService hrContextService,
                         InternRepository internRepository,
                         MentorRepository mentorRepository,
                         HrRepository hrRepository) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationScoreRepository = evaluationScoreRepository;
        this.mentorContextService = mentorContextService;
        this.hrContextService = hrContextService;
        this.internRepository = internRepository;
        this.mentorRepository = mentorRepository;
        this.hrRepository = hrRepository;
    }

    // ============================================================
    // 📋 Lấy tất cả evaluations của một intern
    // ============================================================
    public List<EvaluationResponse> getEvaluationsByInternId(Long internId) {
        List<Evaluation> evaluations = evaluationRepository.findByIntern_Id(internId);

        return evaluations.stream().map(e -> EvaluationResponse.builder()
                .evaluationId(e.getEvaluationId())
                .internId(e.getIntern().getId())
                .internName(e.getIntern().getFullName())
                .comment(e.getComment())
                .cycle(e.getCycle()) // hoặc getCycleType nếu entity có tên khác
                .periodNo(e.getPeriodNo())
                .scores(e.getScores() != null
                        ? e.getScores().stream()
                        .map(s -> new EvaluationScoreResponse(
                                s.getCriteriaName(),
                                s.getScore(),
                                s.getComment()
                        ))
                        .toList()
                        : null)
                .build()
        ).toList();
    }

    // ============================================================
    // 🧠 MENTOR đánh giá Intern (tạo mới)
    // ============================================================
    public EvaluationResponse createMentorEvaluation(EvaluationRequest request) {
        Long mentorId = mentorContextService.getMentorIdFromUserId(request.getUserId());
        if (mentorId == null) {
            throw new RuntimeException("Không tìm thấy mentor tương ứng với userId: " + request.getUserId());
        }

        InternProfile intern = internRepository.findById(request.getInternId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy intern có ID: " + request.getInternId()));

        Mentors mentor = mentorRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mentor có ID: " + mentorId));

        Evaluation evaluation = new Evaluation();
        evaluation.setMentorEvaluator(mentor);
        evaluation.setIntern(intern);
        evaluation.setComment(request.getComment());
        evaluation.setCycle(request.getCycle());
        evaluation.setPeriodNo(request.getPeriodNo());
        evaluation.setCreatedAt(LocalDateTime.now());

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);

        // Lưu các score
        if (request.getScores() != null && !request.getScores().isEmpty()) {
            var scoreEntities = request.getScores().stream().map(scoreReq -> {
                EvaluationScore s = new EvaluationScore();
                s.setEvaluation(savedEvaluation);
                s.setCriteriaName(scoreReq.getCriteriaName());
                s.setScore(scoreReq.getScore());
                s.setComment(scoreReq.getComment());
                return s;
            }).collect(Collectors.toList());
            evaluationScoreRepository.saveAll(scoreEntities);
        }

        // Lấy lại danh sách score sau khi lưu
        List<EvaluationScore> scores = evaluationScoreRepository.findByEvaluation_EvaluationId(savedEvaluation.getEvaluationId());

        return EvaluationResponse.builder()
                .evaluationId(savedEvaluation.getEvaluationId())
                .internId(intern.getId())
                .internName(intern.getFullName())
                .comment(savedEvaluation.getComment())
                .cycle(savedEvaluation.getCycle())
                .periodNo(savedEvaluation.getPeriodNo())
                .mentorName(mentor.getFullName())
                .createdAt(savedEvaluation.getCreatedAt())
                .scores(scores.stream()
                        .map(s -> new EvaluationScoreResponse(
                                s.getCriteriaName(),
                                s.getScore(),
                                s.getComment()))
                        .collect(Collectors.toList()))
                .build();
    }

    // ============================================================
    // ✏️ MENTOR cập nhật evaluation
    // ============================================================
    public EvaluationResponse updateMentorEvaluation(Long evaluationId, EvaluationRequest request) {
        Long mentorId = mentorContextService.getMentorIdFromUserId(request.getUserId());
        if (mentorId == null) {
            throw new RuntimeException("Không tìm thấy mentor tương ứng với userId: " + request.getUserId());
        }

        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy evaluation có ID: " + evaluationId));

        // Kiểm tra quyền
        if (evaluation.getMentorEvaluator() == null ||
                !evaluation.getMentorEvaluator().getId().equals(mentorId)) {
            throw new RuntimeException("Bạn không có quyền sửa evaluation này!");
        }

        // Cập nhật thông tin
        evaluation.setComment(request.getComment());
        evaluation.setCycle(request.getCycle());
        evaluation.setPeriodNo(request.getPeriodNo());
        evaluationRepository.save(evaluation);

        // Xóa score cũ
        evaluationScoreRepository.deleteByEvaluation_EvaluationId(evaluationId);

        // Thêm score mới
        if (request.getScores() != null && !request.getScores().isEmpty()) {
            var newScores = request.getScores().stream().map(scoreReq -> {
                EvaluationScore s = new EvaluationScore();
                s.setEvaluation(evaluation);
                s.setCriteriaName(scoreReq.getCriteriaName());
                s.setScore(scoreReq.getScore());
                s.setComment(scoreReq.getComment());
                return s;
            }).collect(Collectors.toList());
            evaluationScoreRepository.saveAll(newScores);
        }

        List<EvaluationScore> scores = evaluationScoreRepository.findByEvaluation_EvaluationId(evaluationId);

        return EvaluationResponse.builder()
                .evaluationId(evaluationId)
                .internId(evaluation.getIntern().getId())
                .internName(evaluation.getIntern().getFullName())
                .comment(evaluation.getComment())
                .cycle(evaluation.getCycle())
                .periodNo(evaluation.getPeriodNo())
                .mentorName(evaluation.getMentorEvaluator().getFullName())
                .createdAt(evaluation.getCreatedAt())
                .scores(scores.stream()
                        .map(s -> new EvaluationScoreResponse(
                                s.getCriteriaName(),
                                s.getScore(),
                                s.getComment()))
                        .collect(Collectors.toList()))
                .build();
    }

    // ============================================================
    // ❌ MENTOR xóa evaluation
    // ============================================================
    public void deleteMentorEvaluation(Long evaluationId, Long userId) {
        Long mentorId = mentorContextService.getMentorIdFromUserId(userId);
        if (mentorId == null) {
            throw new RuntimeException("Không tìm thấy mentor tương ứng với userId: " + userId);
        }

        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy evaluation có ID: " + evaluationId));

        if (evaluation.getMentorEvaluator() == null ||
                !evaluation.getMentorEvaluator().getId().equals(mentorId)) {
            throw new RuntimeException("Bạn không có quyền xóa evaluation này!");
        }

        evaluationScoreRepository.deleteByEvaluation_EvaluationId(evaluationId);
        evaluationRepository.delete(evaluation);
    }
}
