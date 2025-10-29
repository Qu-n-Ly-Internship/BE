package com.example.be.service;

import com.example.be.dto.EvaluationRequest;
import com.example.be.dto.EvaluationReponse;
import com.example.be.entity.*;
import com.example.be.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    // 🧠 MENTOR đánh giá Intern (theo weekly hoặc monthly)
    // ============================================================
    public EvaluationReponse createMentorEvaluation(EvaluationRequest request) {
        // Lấy mentorId từ userId
        Long mentorId = mentorContextService.getMentorIdFromUserId(request.getUserId());
        if (mentorId == null) {
            throw new RuntimeException("Không tìm thấy mentor tương ứng với userId: " + request.getUserId());
        }

        // Lấy thông tin intern
        InternProfile intern = internRepository.findById(request.getInternId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy intern có ID: " + request.getInternId()));

        // Lấy thông tin mentor
        Mentors mentor = mentorRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mentor có ID: " + mentorId));

        // Tạo evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setMentorEvaluator(mentor);
        evaluation.setIntern(intern);
        evaluation.setComment(request.getComment());
        evaluation.setCycle(request.getCycle());
        evaluation.setPeriodNo(request.getPeriodNo());
        evaluation.setCreatedAt(LocalDateTime.now());

        // Lưu evaluation
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

        // Build response (chỉ trả về dữ liệu cần thiết)
        return EvaluationReponse.builder()
                .evaluationId(savedEvaluation.getEvaluationId())
                .comment(savedEvaluation.getComment())
                .cycle(savedEvaluation.getCycle())
                .periodNo(savedEvaluation.getPeriodNo())
                .createdAt(savedEvaluation.getCreatedAt())
                .mentorName(mentor.getFullName())
                .hrName(null)
                .scores(scores.stream()
                        .map(s -> new EvaluationReponse.ScoreResponse(s.getCriteriaName(), s.getScore(), s.getComment()))
                        .collect(Collectors.toList()))
                .build();
    }

    // ============================================================
    // 🧠 HR đánh giá Intern (cố định monthly)
    // ============================================================
    public EvaluationReponse createHrEvaluation(EvaluationRequest request) {
        // Lấy hrId từ userId
        Long hrId = hrContextService.getHrIdFromUserId(request.getUserId());
        if (hrId == null) {
            throw new RuntimeException("Không tìm thấy HR tương ứng với userId: " + request.getUserId());
        }

        // Lấy thông tin intern
        InternProfile intern = internRepository.findById(request.getInternId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy intern có ID: " + request.getInternId()));

        // Lấy thông tin HR
        Hr hr = hrRepository.findById(hrId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy HR có ID: " + hrId));

        // Tạo evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setHrEvaluator(hr);
        evaluation.setIntern(intern);
        evaluation.setComment(request.getComment());
        evaluation.setCycle("monthly"); // HR luôn theo tháng
        evaluation.setPeriodNo(request.getPeriodNo());
        evaluation.setCreatedAt(LocalDateTime.now());

        // Lưu evaluation
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

        // Build response
        return EvaluationReponse.builder()
                .evaluationId(savedEvaluation.getEvaluationId())
                .comment(savedEvaluation.getComment())
                .cycle(savedEvaluation.getCycle())
                .periodNo(savedEvaluation.getPeriodNo())
                .createdAt(savedEvaluation.getCreatedAt())
                .mentorName(null)
                .hrName(hr.getFullname())
                .scores(scores.stream()
                        .map(s -> new EvaluationReponse.ScoreResponse(s.getCriteriaName(), s.getScore(), s.getComment()))
                        .collect(Collectors.toList()))
                .build();
    }
}
