package com.example.be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send CV approval notification email to intern
     */
    public void sendCVApprovalEmail(String internEmail, String internName, String cvFileName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(internEmail);
            message.setSubject("CV Approval Notification - " + cvFileName);
            
            String emailContent = buildCVApprovalEmailContent(internName, cvFileName);
            message.setText(emailContent);
            
            mailSender.send(message);
            System.out.println("✅ CV approval email sent to: " + internEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send CV approval email to " + internEmail + ": " + e.getMessage());
            throw new RuntimeException("Failed to send CV approval email", e);
        }
    }

    /**
     * Send CV rejection notification email to intern
     */
    public void sendCVRejectionEmail(String internEmail, String internName, String cvFileName, String rejectionReason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(internEmail);
            message.setSubject("CV Rejection Notification - " + cvFileName);
            
            String emailContent = buildCVRejectionEmailContent(internName, cvFileName, rejectionReason);
            message.setText(emailContent);
            
            mailSender.send(message);
            System.out.println("✅ CV rejection email sent to: " + internEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send CV rejection email to " + internEmail + ": " + e.getMessage());
            throw new RuntimeException("Failed to send CV rejection email", e);
        }
    }


    private String buildCVApprovalEmailContent(String internName, String cvFileName) {
        return String.format("""
            Dear %s,
            
            Congratulations! Your CV "%s" has been approved by our HR team.
            
            Your CV meets our requirements and you can proceed with the next steps in the application process.
            
            If you have any questions, please don't hesitate to contact our HR team.
            
            Best regards,
            HR Team
            Internship Management System
            """, internName, cvFileName);
    }

    private String buildCVRejectionEmailContent(String internName, String cvFileName, String rejectionReason) {
        return String.format("""
            Dear %s,
            
            Thank you for submitting your CV "%s" for review.
            
            Unfortunately, your CV has been rejected for the following reason:
            %s
            
            Please review the feedback and feel free to submit an updated CV.
            
            If you have any questions, please don't hesitate to contact our HR team.
            
            Best regards,
            HR Team
            Internship Management System
            """, internName, cvFileName, rejectionReason);
    }



    /**
     * Send generic notification email
     */
    public void sendGenericNotificationEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            System.out.println("✅ Generic notification email sent to: " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send generic notification email to " + to + ": " + e.getMessage());
            throw new RuntimeException("Failed to send generic notification email", e);
        }
    }
}
