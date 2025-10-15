//package com.example.be.controller;
//
//import com.example.be.service.EmailService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/email")
//@RequiredArgsConstructor
//public class EmailController {
//
//    private final EmailService emailService;
//
//    @PostMapping("/send-approval")
//    public ResponseEntity<?> sendApprovalEmail(@RequestBody Map<String, String> request) {
//        try {
//            String to = request.get("to");
//            String documentType = request.get("documentType");
//            String notes = request.get("notes");
//
//            if (to == null || to.trim().isEmpty()) {
//                return ResponseEntity.badRequest().body(Map.of(
//                        "success", false,
//                        "message", "Email recipient is required"
//                ));
//            }
//
//            // For now, we'll use a generic name since we don't have the intern name in the request
//            // In a real implementation, you might want to fetch this from the database
//            String internName = "Intern";
//
//            if ("CV".equalsIgnoreCase(documentType)) {
//                emailService.sendCVApprovalEmail(to, internName, "CV");
//            }
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "message", "Approval email sent successfully to " + to
//            ));
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "Failed to send approval email: " + e.getMessage()
//            ));
//        }
//    }
//
//    @PostMapping("/send-rejection")
//    public ResponseEntity<?> sendRejectionEmail(@RequestBody Map<String, String> request) {
//        try {
//            String to = request.get("to");
//            String documentType = request.get("documentType");
//            String notes = request.get("notes");
//
//            if (to == null || to.trim().isEmpty()) {
//                return ResponseEntity.badRequest().body(Map.of(
//                        "success", false,
//                        "message", "Email recipient is required"
//                ));
//            }
//
//            if (notes == null || notes.trim().isEmpty()) {
//                return ResponseEntity.badRequest().body(Map.of(
//                        "success", false,
//                        "message", "Rejection reason is required"
//                ));
//            }
//
//            // For now, we'll use a generic name since we don't have the intern name in the request
//            String internName = "Intern";
//
//            if ("CV".equalsIgnoreCase(documentType)) {
//                emailService.sendCVRejectionEmail(to, internName, "CV", notes);
//            }
//
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "message", "Rejection email sent successfully to " + to
//            ));
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "Failed to send rejection email: " + e.getMessage()
//            ));
//        }
//    }
//
//    @PostMapping("/send-notification")
//    public ResponseEntity<?> sendNotificationEmail(@RequestBody Map<String, String> request) {
//        try {
//            String to = request.get("to");
//            String subject = request.get("subject");
//            String content = request.get("content");
//            String template = request.get("template");
//
//            if (to == null || to.trim().isEmpty()) {
//                return ResponseEntity.badRequest().body(Map.of(
//                        "success", false,
//                        "message", "Email recipient is required"
//                ));
//            }
//
//            if (subject == null || subject.trim().isEmpty()) {
//                return ResponseEntity.badRequest().body(Map.of(
//                        "success", false,
//                        "message", "Email subject is required"
//                ));
//            }
//
//            if (content == null || content.trim().isEmpty()) {
//                return ResponseEntity.badRequest().body(Map.of(
//                        "success", false,
//                        "message", "Email content is required"
//                ));
//            }
//
//            // This is a generic notification email
//            // You can extend this to support different templates
//            emailService.sendGenericNotificationEmail(to, subject, content);
//
//            return ResponseEntity.ok(Map.of(
//                    "success", true,
//                    "message", "Notification email sent successfully to " + to
//            ));
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "Failed to send notification email: " + e.getMessage()
//            ));
//        }
//    }
//}
