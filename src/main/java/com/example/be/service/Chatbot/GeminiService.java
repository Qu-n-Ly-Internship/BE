package com.example.be.service.Chatbot;

import com.example.be.entity.ChatHistory;
import com.example.be.entity.User;
import com.example.be.repository.ChatHistoryRepository;
import com.example.be.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final String BASE_GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ChatHistoryRepository chatHistoryRepository;
    private final UserRepository userRepository;

    public GeminiService(RestTemplate restTemplate,
                         ChatHistoryRepository chatHistoryRepository,
                         UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.chatHistoryRepository = chatHistoryRepository;
        this.userRepository = userRepository;
    }

    public String getGeminiAnswer(String userPrompt, Long userId, String conversationId) {

        // --- Load previous chat history ---
        List<ChatHistory> historyList = chatHistoryRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<Map<String, Object>> contents = new ArrayList<>();

        for (ChatHistory history : historyList) {
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", history.getUserRequest()))
            ));
            contents.add(Map.of(
                    "role", "model",
                    "parts", List.of(Map.of("text", history.getGeminiResponse()))
            ));
        }

        // --- Add current user prompt ---
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userPrompt))
        ));

        Map<String, Object> body = Map.of("contents", contents);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String finalUrl = UriComponentsBuilder.fromHttpUrl(BASE_GEMINI_API_URL)
                .queryParam("key", apiKey)
                .toUriString();

        String geminiResponseText;

        // --- Call Gemini API ---
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    finalUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> candidates =
                        (List<Map<String, Object>>) response.getBody().get("candidates");

                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
                    geminiResponseText = parts.get(0).get("text");
                } else {
                    geminiResponseText = "Error: Gemini returned an empty response.";
                }
            } else {
                geminiResponseText = "Error calling Gemini API. Status: " + response.getStatusCode();
            }

        } catch (Exception e) {
            geminiResponseText = "Error during API call: " + e.getMessage();
        }

        // --- Save chat history ---
        try {
            ChatHistory history = new ChatHistory();
            history.setConversationId(conversationId);
            history.setUserRequest(userPrompt);
            history.setGeminiResponse(geminiResponseText);
            history.setCreatedAt(LocalDateTime.now());

            // ✅ Nếu có userId thì gán User, nếu không thì để null
            if (userId != null) {
                userRepository.findById(userId).ifPresent(history::setUser);
            } else {
                history.setUser(null);
            }

            chatHistoryRepository.save(history);

        } catch (Exception e) {
            System.err.println("Error saving chat history: " + e.getMessage());
        }

        return geminiResponseText;
    }
}
