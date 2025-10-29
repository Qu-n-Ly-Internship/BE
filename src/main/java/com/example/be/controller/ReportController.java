package com.example.be.controller;

import com.example.be.dto.EvaluationRequest;
import com.example.be.dto.EvaluationReponse; // Vẫn cần
import com.example.be.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ✅ MENTOR đánh giá Intern (weekly / monthly)
    @PostMapping("/mentor")
    // Sửa kiểu trả về thành ResponseEntity<?>
    public ResponseEntity<?> createMentorEvaluation(@RequestBody EvaluationRequest request) {
        try {
            // Service trả về EvaluationReponse
            EvaluationReponse evaluationResponse = reportService.createMentorEvaluation(request);
            return ResponseEntity.ok(evaluationResponse);
        } catch (RuntimeException e) {
            // Trả về String trong trường hợp lỗi (status code 400 Bad Request)
            return ResponseEntity.badRequest().body(" Mentor evaluation failed: " + e.getMessage());
        }
    }

    // ✅ HR đánh giá Intern (chỉ monthly)
    @PostMapping("/hr")
    // Sửa kiểu trả về thành ResponseEntity<?>
    public ResponseEntity<?> createHrEvaluation(@RequestBody EvaluationRequest request) {
        try {
            // Service trả về EvaluationReponse
            EvaluationReponse evaluationResponse = reportService.createHrEvaluation(request);
            return ResponseEntity.ok(evaluationResponse);
        } catch (RuntimeException e) {
            // Trả về String trong trường hợp lỗi (status code 400 Bad Request)
            return ResponseEntity.badRequest().body(" HR evaluation failed: " + e.getMessage());
        }
    }
}