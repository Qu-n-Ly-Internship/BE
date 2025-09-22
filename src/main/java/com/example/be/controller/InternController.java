package com.example.be.controller;

import com.example.be.entity.InternProfile;
import com.example.be.repository.InternProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/interns")
@RequiredArgsConstructor
public class InternController {

    private final InternProfileRepository internProfileRepository;

    // Lấy danh sách tất cả intern profiles
    @GetMapping
    public ResponseEntity<?> getAllInterns(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<InternProfile> interns = internProfileRepository.findAll(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("content", interns.getContent());
            response.put("totalElements", interns.getTotalElements());
            response.put("totalPages", interns.getTotalPages());
            response.put("currentPage", page);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tải danh sách intern: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Tạo intern profile mới
    @PostMapping
    public ResponseEntity<?> createIntern(@RequestBody InternProfile internProfile) {
        try {
            InternProfile savedIntern = internProfileRepository.save(internProfile);
            return ResponseEntity.ok(savedIntern);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể tạo intern profile: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Lấy thông tin intern theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getInternById(@PathVariable Long id) {
        try {
            var internOpt = internProfileRepository.findById(id);
            if (internOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Không tìm thấy intern với ID: " + id);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(internOpt.get());

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Lỗi khi tải thông tin intern: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Cập nhật intern profile
    @PutMapping("/{id}")
    public ResponseEntity<?> updateIntern(@PathVariable Long id, @RequestBody InternProfile internProfile) {
        try {
            if (!internProfileRepository.existsById(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Không tìm thấy intern với ID: " + id);
                return ResponseEntity.notFound().build();
            }

            internProfile.setInternId(id);
            InternProfile updatedIntern = internProfileRepository.save(internProfile);
            return ResponseEntity.ok(updatedIntern);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể cập nhật intern: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Xóa intern profile
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteIntern(@PathVariable Long id) {
        try {
            if (!internProfileRepository.existsById(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Không tìm thấy intern với ID: " + id);
                return ResponseEntity.notFound().build();
            }

            internProfileRepository.deleteById(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Xóa intern thành công");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Không thể xóa intern: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}