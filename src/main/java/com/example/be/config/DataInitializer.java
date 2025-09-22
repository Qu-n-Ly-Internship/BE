package com.example.be.config;

import com.example.be.entity.User;
import com.example.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) throws Exception {
        // Tạo admin account cứng nếu chưa tồn tại
        if (userRepository.findByEmail("admin@company.com").isEmpty()) {
            User admin = new User();
            admin.setFullName("System Administrator");
            admin.setEmail("admin@company.com");
            admin.setUsername("admin@company.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setStatus("ACTIVE");

            userRepository.save(admin);
            System.out.println("✅ Đã tạo admin account: admin@company.com / admin123");
        }


    }

    private void createDemoUserIfNotExists(String email, String password, String fullName, String role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setUsername(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            user.setStatus("ACTIVE");

            userRepository.save(user);
            System.out.println("✅ Đã tạo " + role + " account: " + email + " / " + password);
        }
    }
}