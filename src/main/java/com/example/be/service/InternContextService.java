package com.example.be.service;

import com.example.be.entity.InternProfile;
import com.example.be.entity.User;
import com.example.be.repository.InternRepository;
import com.example.be.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class InternContextService {

    private final InternRepository internRepository;
    private final UserRepository userRepository;

    public InternContextService(InternRepository internRepository, UserRepository userRepository) {
        this.internRepository = internRepository;
        this.userRepository = userRepository;
    }

    // ✅ Lấy internId từ userId
    public Long getInternIdFromUserId(Long userId) {
        // Kiểm tra user có tồn tại không
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với id = " + userId));

        // Tìm intern tương ứng với user
        return internRepository.findByUser_Id(userId)
                .map(InternProfile::getId)
                .orElse(null);
    }
}
