package com.example.be.notification.service;

import com.example.be.notification.entity.Notification;
import com.example.be.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic;
    private final NotificationRepository repository;

    /**
     * Gửi notification: Lưu DB + Publish Redis
     */
    @Transactional
    public void publish(String userId, String type, String title, String message) {
        try {
            // 1. Lưu vào DB trước (để có log)
            Notification notification = repository.save(Notification.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .message(message)
                    .build());

            // 2. Push realtime qua Redis
            redisTemplate.convertAndSend(topic.getTopic(), notification);

            log.info("✅ Notification sent: userId={}, type={}", userId, type);

        } catch (Exception e) {
            log.error("❌ Failed to publish notification: {}", e.getMessage());
            // Không throw exception để không ảnh hưởng business logic
        }
    }
}
