package com.example.be.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final Map<String, String> emailTemplates = new HashMap<>();

    @Value("${email.template.file}")
    private Resource templateFile;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ✅ Thêm @PostConstruct để load sau khi Spring inject xong @Value
    @PostConstruct
    public void init() {
        loadTemplatesFromCsv();
    }

    private void loadTemplatesFromCsv() {
        try (CSVReader reader = new CSVReader(new InputStreamReader(templateFile.getInputStream()))) {
            String[] line;
            reader.readNext(); // skip header
            while ((line = reader.readNext()) != null) {
                if (line.length >= 3) {
                    String tag = line[1].trim();
                    String value = line[2].trim();
                    emailTemplates.put(tag, value);
                }
            }
            logger.info("✅ Loaded email templates: {}", emailTemplates);
        } catch (IOException | CsvValidationException e) {
            logger.error("❌ Error loading email templates", e);
        }
    }

    public void sendStatusEmail(String toEmail, String fullName, String statusTag, String reason) throws MessagingException {
        String subject = emailTemplates.getOrDefault(statusTag, "Kết quả duyệt hồ sơ");
        String content = buildEmailContent(fullName, statusTag, reason);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
        logger.info("📧 Sent email to {} with status {}", toEmail, statusTag);
    }

    private String buildEmailContent(String fullName, String statusTag, String reason) {
        String statusValue = emailTemplates.getOrDefault(statusTag, "Không xác định");
        String body = "<html><body>" +
                "<p>Kính gửi " + fullName + ",</p>" +
                "<p>Kết quả duyệt hồ sơ thực tập của bạn: <strong>" + statusValue + "</strong>.</p>";
        if ("rejected".equalsIgnoreCase(statusTag) && reason != null && !reason.isEmpty()) {
            body += "<p>Lý do từ chối: " + reason + "</p>";
        }
        body += "<p>Hướng dẫn tiếp theo: [Thêm chi tiết dựa trên status].</p>" +
                "<p>Trân trọng,</p><p>Đội ngũ HR</p>" +
                "</body></html>";
        return body;
    }
}
