package com.example.be.service;

import com.example.be.entity.Email;
import com.example.be.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailRepository EmailRepository;

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
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
