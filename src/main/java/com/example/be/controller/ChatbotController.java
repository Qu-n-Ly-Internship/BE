package com.example.be.controller;

import com.example.be.service.Chatbot.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final GeminiService geminiService;

    public ChatbotController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/ask")
    public ResponseEntity<?> askGemini(@RequestBody Map<String, String> request) {

        String userPrompt = request.get("prompt");
        String conversationId = request.get("conversationId");
        String userIdStr = request.get("userId");

        // --- Kiểm tra prompt ---
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt không được để trống."));
        }

        // --- Nếu không có conversationId thì tự tạo ---
        if (conversationId == null || conversationId.trim().isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }

        // --- Xử lý userId (có thể null hoặc rỗng) ---
        Long userId = null;
        if (userIdStr != null && !userIdStr.trim().isEmpty()) {
            try {
                userId = Long.valueOf(userIdStr);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "User ID không hợp lệ."));
            }
        }

        // --- Gọi service ---
        String answer = geminiService.getGeminiAnswer(userPrompt, userId, conversationId);

        // --- Trả kết quả ---
        return ResponseEntity.ok(Map.of(
                "conversationId", conversationId,
                "response", answer
        ));
    }
}
