package com.example.be.service;

import com.example.be.dto.*;
import com.example.be.entity.*;
import com.example.be.notification.service.NotificationPublisher;
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
    private final ReportRepository reportRepository;
    private final InternContextService internContextService;
    private final NotificationPublisher notificationPublisher;

    public ReportService(EvaluationRepository evaluationRepository,
                         EvaluationScoreRepository evaluationScoreRepository,
                         MentorContextService mentorContextService,
                         HrContextService hrContextService,
                         InternRepository internRepository,
                         MentorRepository mentorRepository,
                         HrRepository hrRepository,
                         ReportRepository reportRepository,
                         InternContextService internContextService,
                         NotificationPublisher notificationPublisher) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationScoreRepository = evaluationScoreRepository;
        this.mentorContextService = mentorContextService;
        this.hrContextService = hrContextService;
        this.internRepository = internRepository;
        this.mentorRepository = mentorRepository;
        this.hrRepository = hrRepository;
        this.reportRepository = reportRepository;
        this.internContextService = internContextService;
        this.notificationPublisher = notificationPublisher;
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
                .cycle(e.getCycle())
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

        notificationPublisher.publish(
                intern.getId().toString(), // chỉ gửi cho intern
                "MENTOR_EVALUATION",
                "Bạn vừa bị đánh giá",
                "Mentor " + mentor.getFullName() + " đã gửi đánh giá cho bạn. Evaluation ID: " + savedEvaluation.getEvaluationId()
        );

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

    // ✅ Tạo report (frontend gửi userId và dùng DTO ReportRequest)
    public ReportResponse createReport(ReportRequest req, Long userId) {
        // Dùng HrContext để map userId → hrId
        Long hrId = hrContextService.getHrIdFromUserId(userId);
        if (hrId == null) {
            throw new RuntimeException("Không tìm thấy HR tương ứng với userId = " + userId);
        }

        Hr hr = hrRepository.findById(hrId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy HR với id = " + hrId));

        InternProfile intern = internRepository.findById(req.getInternId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thực tập sinh với id = " + req.getInternId()));

        Report report = new Report();
        report.setHr(hr);
        report.setIntern(intern);
        report.setOverallScore(req.getOverallScore());
        report.setSummary(req.getSummary());
        report.setRecommendations(req.getRecommendations());

        report.setCreatedAt(LocalDateTime.now());

        Report saved = reportRepository.save(report);

        notificationPublisher.publish(
                intern.getId().toString(), // chỉ gửi cho intern
                "REPORT_CREATED",
                "Báo cáo mới đã được tạo",
                "HR " + hr.getFullname() + " vừa tạo báo cáo về bạn. Report ID: " + saved.getReportId()
        );

        return mapToResponse(saved);
    }


    // ✅ Cập nhật report
    public ReportResponse updateReport(Long id, ReportRequest req) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy report"));

        report.setOverallScore(req.getOverallScore());
        report.setSummary(req.getSummary());
        report.setRecommendations(req.getRecommendations());


        Report updated = reportRepository.save(report);
        return mapToResponse(updated);
    }

    // ✅ Xóa report
    public void deleteReport(Long id) {
        if (!reportRepository.existsById(id)) {
            throw new RuntimeException("Report không tồn tại");
        }
        reportRepository.deleteById(id);
    }

    // ✅ Lấy tất cả report của 1 intern
    public List<ReportResponse> getReportsByIntern(Long internId) {
        return reportRepository.findByIntern_Id(internId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ✅ Xem chi tiết 1 report
    public ReportResponse getReportById(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy report"));
        return mapToResponse(report);
    }

    // --- Chuyển Entity → DTO ---
    private ReportResponse mapToResponse(Report r) {
        return ReportResponse.builder()
                .reportId(r.getReportId())
                .hrName(r.getHr().getFullname())
                .internName(r.getIntern().getFullName())
                .overallScore(r.getOverallScore())
                .summary(r.getSummary())
                .recommendations(r.getRecommendations())

                .createdAt(r.getCreatedAt())
                .build();
    }

    // ✅ Lấy tất cả evaluation của intern (thông qua userId)
    public List<EvaluationResponse> getAllEvaluationsByUserId(Long userId) {
        Long internId = internContextService.getInternIdFromUserId(userId);
        if (internId == null) {
            throw new RuntimeException("Không tìm thấy intern cho userId = " + userId);
        }

        List<Evaluation> evaluations = evaluationRepository.findByIntern_Id(internId);

        return evaluations.stream().map(EvaluationResponse::fromEntity).collect(Collectors.toList());
    }

    // ✅ Lấy tất cả report của intern (thông qua userId)
    public List<ReportResponse> getAllReportsByUserId(Long userId) {
        Long internId = internContextService.getInternIdFromUserId(userId);
        if (internId == null) {
            throw new RuntimeException("Không tìm thấy intern cho userId = " + userId);
        }

        List<Report> reports = reportRepository.findByIntern_Id(internId);

        return reports.stream().map(ReportResponse::fromEntity).collect(Collectors.toList());
    }

}
