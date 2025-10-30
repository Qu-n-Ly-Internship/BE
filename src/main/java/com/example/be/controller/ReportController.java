package com.example.be.controller;

import com.example.be.dto.EvaluationRequest;
import com.example.be.dto.EvaluationResponse;
import com.example.be.dto.ReportRequest;
import com.example.be.dto.ReportResponse;
import com.example.be.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }


    @GetMapping("/intern/{internId}/evaluations")
    public ResponseEntity<List<EvaluationResponse>> getEvaluationsByIntern(@PathVariable Long internId) {
        List<EvaluationResponse> responses = reportService.getEvaluationsByInternId(internId);
        return ResponseEntity.ok(responses);
    }

    // ============================================================
    // 🧠 MENTOR TẠO EVALUATION MỚI
    // ============================================================
    @PostMapping("/mentor")
    public ResponseEntity<EvaluationResponse> createMentorEvaluation(@RequestBody EvaluationRequest request) {
        EvaluationResponse response = reportService.createMentorEvaluation(request);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // ✏️ MENTOR CẬP NHẬT EVALUATION
    // ============================================================
    @PutMapping("/mentor/{evaluationId}")
    public ResponseEntity<EvaluationResponse> updateMentorEvaluation(
            @PathVariable Long evaluationId,
            @RequestBody EvaluationRequest request
    ) {
        EvaluationResponse response = reportService.updateMentorEvaluation(evaluationId, request);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // ❌ MENTOR XÓA EVALUATION
    // ============================================================
    @DeleteMapping("/mentor/{evaluationId}")
    public ResponseEntity<Void> deleteMentorEvaluation(
            @PathVariable Long evaluationId,
            @RequestParam Long userId
    ) {
        reportService.deleteMentorEvaluation(evaluationId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @RequestBody ReportRequest request,
            @RequestParam Long userId
    ) {
        ReportResponse response = reportService.createReport(request, userId);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // ✏️ Cập nhật report
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<ReportResponse> updateReport(
            @PathVariable Long id,
            @RequestBody ReportRequest request
    ) {
        ReportResponse response = reportService.updateReport(id, request);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // ❌ Xóa report
    // ============================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // 📋 Lấy tất cả report của 1 intern
    // ============================================================
    @GetMapping("/intern/{internId}")
    public ResponseEntity<List<ReportResponse>> getReportsByIntern(@PathVariable Long internId) {
        List<ReportResponse> reports = reportService.getReportsByIntern(internId);
        return ResponseEntity.ok(reports);
    }

    // ============================================================
    // 🔍 Xem chi tiết 1 report
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getReportById(@PathVariable Long id) {
        ReportResponse response = reportService.getReportById(id);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/by-user/reports")
    public ResponseEntity<List<ReportResponse>> getReportsByUserQuery(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(reportService.getAllReportsByUserId(userId));
    }

    @GetMapping("/by-user/evaluations")
    public ResponseEntity<List<EvaluationResponse>> getEvaluationsByUserQuery(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(reportService.getAllEvaluationsByUserId(userId));
    }

}
