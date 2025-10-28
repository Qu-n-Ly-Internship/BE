package com.example.be.controller;

import com.example.be.dto.EvaluationRequest;
import com.example.be.dto.EvaluationResponse;
import com.example.be.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @PostMapping("/evaluations")
    public ResponseEntity<EvaluationResponse> create(@RequestBody EvaluationRequest request) {
        return ResponseEntity.ok(reportService.createEvaluation(request));
    }

    @PutMapping("/evaluations/{id}")
    public ResponseEntity<EvaluationResponse> update(
            @PathVariable Long id, @RequestBody EvaluationRequest request) {
        return ResponseEntity.ok(reportService.updateEvaluation(id, request));
    }

    @DeleteMapping("/evaluations/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reportService.deleteEvaluation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/intern/{internId}")
    public ResponseEntity<List<EvaluationResponse>> getByIntern(@PathVariable Long internId) {
        return ResponseEntity.ok(reportService.getEvaluationsByIntern(internId));
    }
}
