package com.example.be.controller;

import com.example.be.dto.EvaluationRequest;
import com.example.be.dto.EvaluationResponse;
import com.example.be.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*") // ⚙️ Cho phép frontend truy cập (nếu cần)
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ============================================================
    // 🧩 LẤY DANH SÁCH CÁC EVALUATION CỦA MỘT INTERN
    // ============================================================
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
}
