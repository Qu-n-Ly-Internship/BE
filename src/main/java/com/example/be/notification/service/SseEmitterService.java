package com.example.be.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseEmitterService {

    // Lưu connection của từng user
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Tạo SSE connection cho user
     */
    public SseEmitter createEmitter(String userId) {
        // Timeout 1 giờ
        SseEmitter emitter = new SseEmitter(3600000L);

        emitters.put(userId, emitter);
        log.info("🔗 SSE connected: userId={}, total={}", userId, emitters.size());

        // Cleanup khi disconnect
        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.info("👋 SSE disconnected: userId={}", userId);
        });

        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.warn("⏰ SSE timeout: userId={}", userId);
        });

        emitter.onError((ex) -> {
            emitters.remove(userId);
            log.error("❌ SSE error: userId={}, error={}", userId, ex.getMessage());
        });

        return emitter;
    }

    /**
     * Gửi notification tới user
     */
    public void sendToUser(String userId, Object data) {
        SseEmitter emitter = emitters.get(userId);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(data));

                log.info("✉️ Sent to user: userId={}", userId);

            } catch (IOException e) {
                emitters.remove(userId);
                log.error("❌ Failed to send: userId={}", userId);
            }
        } else {
            log.debug("⚠️ User not connected: userId={}", userId);
        }
    }

    /**
     * Heartbeat mỗi 30s để giữ connection
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                emitters.remove(userId);
                log.warn("💔 Heartbeat failed: userId={}", userId);
            }
        });

        if (!emitters.isEmpty()) {
            log.debug("💓 Heartbeat sent to {} users", emitters.size());
        }
    }

    /**
     * Broadcast tới tất cả users (admin notification)
     */
    public void broadcast(Object data) {
        emitters.forEach((userId, emitter) -> sendToUser(userId, data));
    }
}
