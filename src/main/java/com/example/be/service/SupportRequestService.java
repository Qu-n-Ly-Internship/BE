package com.example.be.service;

import com.example.be.dto.SupportRequestDTO;
import com.example.be.entity.SupportRequest;
import com.example.be.repository.SupportRequestRepository;
import com.example.be.repository.InternProfileRepository;
import com.example.be.entity.InternProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupportRequestService {
    private final SupportRequestRepository supportRequestRepository;
    private final CloudinaryRestService cloudinaryRestService;
    private final InternProfileRepository internProfileRepository; // ⭐ THÊM

    public SupportRequest createRequest(SupportRequestDTO dto) {
        SupportRequest request = SupportRequest.builder()
                .internId(dto.getInternId())
                .userId(dto.getUserId()) // ⭐ THÊM
                .subject(dto.getSubject())
                .message(dto.getMessage())
                .attachmentFileId(dto.getAttachmentFileId())
                .priority(dto.getPriority())
                .build();
        return supportRequestRepository.save(request);
    }

    public SupportRequest getRequestById(Long id) {
        return supportRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Support request not found"));
    }

    public List<SupportRequest> getRequestsByIntern(Integer internId) {
        return supportRequestRepository.findByInternId(internId);
    }

    // ⭐ THÊM method mới: Lấy theo user_id
    public List<SupportRequest> getRequestsByUser(Integer userId) {
        return supportRequestRepository.findByUserId(userId);
    }

    public List<SupportRequest> getRequestsByHr(Long hrId) {
        return supportRequestRepository.findByHrId(hrId);
    }

    public Map<String, Object> getAllRequests(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(sortBy != null ? sortBy : "createdAt").descending());
        Page<SupportRequest> pageResult = supportRequestRepository.findAll(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("content", pageResult.getContent());
        response.put("currentPage", pageResult.getNumber());
        response.put("totalItems", pageResult.getTotalElements());
        response.put("totalPages", pageResult.getTotalPages());

        return response;
    }

    public SupportRequest processRequest(Long requestId, Long hrId, String response, String status) {
        SupportRequest request = getRequestById(requestId);
        request.setHrId(hrId);
        request.setHrResponse(response);
        request.setStatus(status);

        LocalDateTime now = LocalDateTime.now();
        if (status.equals("RESOLVED")) {
            request.setResolvedAt(now);
        }
        request.setRespondedAt(now);

        return supportRequestRepository.save(request);
    }

    public List<SupportRequest> getRequestsByStatus(String status) {
        return supportRequestRepository.findByStatus(status);
    }

    // ⭐ CẬP NHẬT: Tạo request với cả internId và userId
    public SupportRequest createRequestWithFile(Integer userId, String subject, String message,
                                                String priority, MultipartFile file) {
        try {
            System.out.println("=== CREATE REQUEST DEBUG ===");
            System.out.println("userId: " + userId);
            System.out.println("subject: " + subject);
            System.out.println("message: " + message);
            System.out.println("priority: " + priority);
            System.out.println("file: " + (file != null ? file.getOriginalFilename() : "null"));

            // ⭐ Tìm intern_id từ user_id
            Integer internId = null;
            try {
                InternProfile profile = internProfileRepository.findByUser_Id(userId.longValue())
                        .orElse(null);
                if (profile != null) {
                    internId = profile.getId().intValue();
                    System.out.println("Found intern_id: " + internId + " for user_id: " + userId);
                } else {
                    System.out.println("No intern profile found for user_id: " + userId);
                }
            } catch (Exception e) {
                System.out.println("Error finding intern_id: " + e.getMessage());
            }

            String fileUrl = null;
            if (file != null && !file.isEmpty()) {
                String cloudinaryResponse = cloudinaryRestService.uploadFile(file);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(cloudinaryResponse);
                fileUrl = jsonNode.get("secure_url").asText();
                System.out.println("File uploaded: " + fileUrl);
            }

            // ⭐ Lưu cả userId và internId
            SupportRequest request = SupportRequest.builder()
                    .userId(userId)
                    .internId(internId) // Có thể null nếu không tìm thấy
                    .subject(subject)
                    .message(message)
                    .attachmentFileId(fileUrl)
                    .priority(priority)
                    .build();

            SupportRequest saved = supportRequestRepository.save(request);
            System.out.println("Request saved with ID: " + saved.getId());
            return saved;

        } catch (Exception e) {
            System.err.println("Error details: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi tạo yêu cầu với file: " + e.getMessage(), e);
        }
    }
}