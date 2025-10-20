package com.example.be.controller;

import com.example.be.service.InternScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class InternScheduleController {

    private final InternScheduleService scheduleService;

    @GetMapping("/{internId}")
    public ResponseEntity<?> getSchedule(@PathVariable Long internId) {
        return ResponseEntity.ok(scheduleService.getScheduleByIntern(internId));
    }

    @PostMapping("/generate/{internId}")
    public ResponseEntity<?> generateSchedule(@PathVariable Long internId) {
        return ResponseEntity.ok(scheduleService.generateSchedule(internId));
    }
}
