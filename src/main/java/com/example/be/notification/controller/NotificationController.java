package com.example.be.notification.controller;

import com.example.be.notification.service.NotificationPublisher;
import com.example.be.notification.repository.NotificationRepository;
import com.example.be.notification.service.SseEmitterService;
import com.example.be.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional; // ← Spring's transactional
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository repository;
    private final SseEmitterService sseEmitterService;
    private final NotificationPublisher publisher;

    @GetMapping("/stream/{userId}")
    public SseEmitter stream(@PathVariable String userId) {
        return sseEmitterService.createEmitter(userId);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Notification>> getAll(
            @PathVariable String userId,
            @RequestParam(required = false) String status) {

        List<Notification> notifications;

        if (status != null) {
            notifications = repository.findByUserIdAndStatus(userId, status);
        } else {
            notifications = repository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String userId) {
        long count = repository.countByUserIdAndStatus(userId, "UNREAD");
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        repository.findById(id).ifPresent(notification -> {
            notification.setStatus("READ");
            repository.save(notification);
        });
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/read-all")
    @Transactional // ← Spring's @Transactional
    public ResponseEntity<Void> markAllAsRead(@PathVariable String userId) {
        repository.markAllAsReadByUserId(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendTest(@RequestBody Map<String, String> payload) {
        publisher.publish(
                payload.get("userId"),
                payload.getOrDefault("type", "TEST"),
                payload.getOrDefault("title", "Test Notification"),
                payload.get("message")
        );
        return ResponseEntity.ok("Notification sent successfully");
    }
}