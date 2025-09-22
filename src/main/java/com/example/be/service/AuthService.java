package com.example.be.service;

import com.example.be.dto.LoginRequest;
import com.example.be.dto.RegisterRequest;
import com.example.be.entity.Role;
import com.example.be.entity.User;
import com.example.be.repository.RoleRepository;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ==================== REGISTER ====================
    public String register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return "Email đã tồn tại!";
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return "Username đã tồn tại!";
        }

        // Tạo user mới
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus("PENDING");

        // Lấy role từ DB, mặc định là INTERN
        String roleName = request.getRole() != null ? request.getRole().toUpperCase() : "INTERN";
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + roleName));
        user.setRole(role);

        userRepository.save(user);
        return "Đăng ký thành công! Role: " + role.getName();
    }

    // ==================== LOGIN (trả String đơn giản) ====================
    public String login(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> {
                    if (!"ACTIVE".equals(user.getStatus())) {
                        return "Tài khoản chưa được duyệt!";
                    }
                    return "Đăng nhập thành công! Xin chào " + user.getUsername()
                            + " (Role: " + user.getRole().getName() + ")";
                })
                .orElse("Sai email hoặc mật khẩu!");
    }

    // ==================== LOGIN (trả object cho frontend) ====================
    public Map<String, Object> loginWithResponse(LoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        var userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Email không tồn tại!");
            return response;
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            response.put("success", false);
            response.put("message", "Mật khẩu không đúng!");
            return response;
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            response.put("success", false);
            response.put("message", "Tài khoản chưa được kích hoạt hoặc đã bị khóa!");
            return response;
        }

        // Mock JWT token
        String token = "mock-jwt-token-" + user.getId() + "-" + System.currentTimeMillis();

        response.put("success", true);
        response.put("message", "Đăng nhập thành công!");
        response.put("token", token);
        response.put("user", Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getRole().getName(),
                "status", user.getStatus()
        ));

        return response;
    }
}
