package com.example.be.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.be.dto.LoginRequest;
import com.example.be.dto.RegisterRequest;
import com.example.be.repository.UserRepository;
import com.example.be.service.JwtService;
import com.example.be.entity.User;
import com.example.be.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", methods = { RequestMethod.GET, RequestMethod.POST,
        RequestMethod.OPTIONS }, allowCredentials = "true")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/register")

    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> result = authService.register(request);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginWithResponse(request));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.replace("Bearer ", "");
        String email = jwtService.extractUsername(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "username", user.getUsername(),
                "fullName", user.getFullName(),
                "role", user.getRole().getName()));
    }

    @GetMapping("/oauth2-login")
    public String oauthLoginPage() {
        return "login-oauth"; // file login-oauth.html trong templates (nếu dùng Thymeleaf/Freemarker)
    }

}