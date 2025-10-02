package com.example.be.service;

import com.example.be.config.JwtUtil;
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
    private final JwtUtil jwtUtil;

    // ==================== REGISTER ====================
    public Map<String, Object> register(RegisterRequest request) {
        try {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return Map.of("success", false, "message", "Email đã tồn tại!");
            }

            // Tạo user mới
            User user = new User();
            user.setEmail(request.getEmail());

            // ✅ FIX: Đảm bảo fullName không bị NULL
            String fullName = request.getFullName();
            if (fullName == null || fullName.trim().isEmpty()) {
                // Nếu không có fullName, tạo từ email
                String emailPrefix = request.getEmail().split("@")[0];
                fullName = emailPrefix.substring(0, 1).toUpperCase() + emailPrefix.substring(1);
            }
            user.setFullName(fullName);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setStatus("ACTIVE"); //
            user.setAuthProvider("LOCAL"); // Set authProvider

            // Lấy role từ DB, luôn mặc định là USER khi đăng ký (tránh leo quyền từ client)
            String roleName = "USER";
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + roleName));
            user.setRole(role);
            
            System.out.println("🔍 DEBUG Register - Email: " + user.getEmail() + 
                             " | Role: " + role.getName() + 
                             " (ID: " + role.getId() + ") | Status: " + user.getStatus());

            User savedUser = userRepository.save(user);
            
            System.out.println("✅ User saved - ID: " + savedUser.getId() + 
                             " | Role: " + savedUser.getRole().getName() + 
                             " | Status: " + savedUser.getStatus());

            // Trả về format đồng nhất với login
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đăng ký thành công! Tài khoản của bạn đang chờ admin duyệt. Bạn sẽ nhận được thông báo khi tài khoản được kích hoạt.");
            response.put("user", Map.of(
                    "id", savedUser.getId(),
                    "fullName", savedUser.getFullName(),
                    "email", savedUser.getEmail(),
                    "status", savedUser.getStatus(),
                    "role", savedUser.getRole().getName()
            ));

            return response;

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "Đăng ký thất bại: " + e.getMessage()
            );
        }
    }

    // ==================== LOGIN (trả String đơn giản) ====================
    public String login(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> {
                    if (!"ACTIVE".equals(user.getStatus())) {
                        return "Tài khoản chưa được duyệt!";
                    }
                    return "Đăng nhập thành công! Xin chào " + user.getEmail() // Sử dụng email thay vì username
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
            if ("PENDING".equals(user.getStatus())) {
                response.put("message", "Tài khoản đang chờ admin duyệt. Vui lòng liên hệ admin để được kích hoạt!");
            } else {
                response.put("message", "Tài khoản đã bị khóa hoặc vô hiệu hóa!");
            }
            return response;
        }

        response.put("success", true);
        response.put("message", "Đăng nhập thành công!");
        response.put("user", Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "status", user.getStatus(),
                "role", user.getRole().getName() // Trả về role name string thay vì object
        ));
        String token = jwtUtil.generateToken(request.getEmail(), user.getRole().getName());
        response.put("token", token);

        return response;
    }
}