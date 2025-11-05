package com.example.be.notification.service;


import com.example.be.notification.entity.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSubscriber implements MessageListener {

    private final SseEmitterService sseEmitterService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // Parse JSON từ Redis
            Notification notification = objectMapper.readValue(
                    message.getBody(),
                    Notification.class
            );

            // Gửi tới user qua SSE
            sseEmitterService.sendToUser(notification.getUserId(), notification);

            log.info("📨 Pushed notification to SSE: userId={}", notification.getUserId());

        } catch (Exception e) {
            log.error("❌ Error processing Redis message: {}", e.getMessage());
        }
    }
}