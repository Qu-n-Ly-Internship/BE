package com.example.be.service;

import com.example.be.entity.Email;
import com.example.be.entity.User;
import com.example.be.notification.service.NotificationPublisher;
import com.example.be.repository.EmailRepository;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailRepository EmailRepository;
    private final NotificationPublisher notificationPublisher;
    private final UserRepository userRepository;

    private String applyPlaceholders(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    public void sendEmailFromTemplate(String to, String templateCode, Map<String, String> placeholders) {
        Email template = EmailRepository.findByCode(templateCode)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateCode));

        String subject = applyPlaceholders(template.getSubject(), placeholders);
        String body = applyPlaceholders(template.getBody(), placeholders);

        sendEmail(to, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            try {
                // Lấy userId của intern để gửi notification
                User user = userRepository.findByEmail(to)
                        .orElseThrow(() -> new RuntimeException("User not found for email: " + to));

                notificationPublisher.publish(
                        user.getId().toString(),
                        "NEW CHANGE",
                        "Trạng Thái CV Đã Thay Đổi",
                        "Chi tiết xem tại gmail đã đăng kí"
                );

                System.out.println("✅ Notification sent to intern userId: " + user.getId());
            } catch (Exception e) {
                // Không throw exception để không ảnh hưởng việc tạo task
                System.err.println("❌ Failed to send notification: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
