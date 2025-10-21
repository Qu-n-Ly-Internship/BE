package com.example.be.service;

import com.example.be.entity.Hr;
import com.example.be.entity.User;
import com.example.be.repository.HrRepository;
import com.example.be.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class HrContextService {

    private final HrRepository hrRepository;
    private final UserRepository userRepository;

    public HrContextService(HrRepository hrRepository, UserRepository userRepository) {
        this.hrRepository = hrRepository;
        this.userRepository = userRepository;
    }

    // ✅ Lấy hrId từ userId
    public Long getHrIdFromUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với id = " + userId));

        Hr hr = hrRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy HR tương ứng với user id = " + userId));

        return hr.getId();
    }
}
