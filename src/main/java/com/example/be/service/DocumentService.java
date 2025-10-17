package com.example.be.service;

import com.example.be.entity.InternDocument;
import com.example.be.entity.InternProfile;
import com.example.be.entity.User;
import com.example.be.repository.InternDocumentRepository;
import com.example.be.repository.InternProfileRepository;
import com.example.be.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final InternDocumentRepository documentRepository;
    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository;
    private final CloudinaryRestService cloudinaryService;
    
    private ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // 1. Lấy tất cả tài liệu với filter
    public Map<String, Object> getAllDocuments(String status, String documentType, String query) {
        try {
            List<InternDocument> documents;

            if (status != null || documentType != null) {
                documents = documentRepository.findDocumentsWithFilters(status, documentType);
            } else {
                documents = documentRepository.findAll();
            }

            // Filter by query if provided
            if (query != null && !query.trim().isEmpty()) {
                String searchQuery = query.toLowerCase();
                documents = documents.stream()
                        .filter(doc -> doc.getDocumentName().toLowerCase().contains(searchQuery) ||
                                (doc.getInternProfile() != null &&
                                        doc.getInternProfile().getFullName().toLowerCase().contains(searchQuery)))
                        .collect(Collectors.toList());
            }

            List<Map<String, Object>> documentList = documents.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());

            return Map.of(
                    "success", true,
                    "total", documentList.size(),
                    "data", documentList
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi lấy danh sách tài liệu: " + e.getMessage());
        }
    }

    // 2. Lấy tài liệu của người dùng
    public Map<String, Object> getMyDocuments(String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user với email: " + email));

            List<InternDocument> documents = documentRepository.findAll().stream()
                    .filter(doc -> doc.getInternProfile() != null &&
                            email.equalsIgnoreCase(doc.getInternProfile().getFullName() + "@company.com"))
                    .collect(Collectors.toList());

            List<Map<String, Object>> documentList = documents.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());

            return Map.of(
                    "success", true,
                    "total", documentList.size(),
                    "data", documentList
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi lấy tài liệu: " + e.getMessage());
        }
    }

    // 3. Lấy tài liệu chờ duyệt
    public Map<String, Object> getPendingDocuments() {
        try {
            List<InternDocument> documents = documentRepository.findPendingDocuments();

            List<Map<String, Object>> documentList = documents.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());

            return Map.of(
                    "success", true,
                    "total", documentList.size(),
                    "data", documentList
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi lấy tài liệu chờ duyệt: " + e.getMessage());
        }
    }

    // 4. Lấy chi tiết tài liệu
    public Map<String, Object> getDocumentById(Long id) {
        try {
            InternDocument document = documentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu với ID: " + id));

            return Map.of(
                    "success", true,
                    "data", convertToMap(document)
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi lấy chi tiết tài liệu: " + e.getMessage());
        }
    }

    // 5. Lấy tài liệu theo intern
    public Map<String, Object> getDocumentsByIntern(Long internId) {
        try {
            List<InternDocument> documents = documentRepository.findByInternProfileId(internId);

            List<Map<String, Object>> documentList = documents.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());

            return Map.of(
                    "success", true,
                    "total", documentList.size(),
                    "data", documentList
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi lấy tài liệu của thực tập sinh: " + e.getMessage());
        }
    }

    // 6. Thống kê tài liệu
    public Map<String, Object> getDocumentStats() {
        try {
            long totalDocuments = documentRepository.count();
            long pendingCount = documentRepository.countByStatus("PENDING");
            long approvedCount = documentRepository.countByStatus("APPROVED");
            long rejectedCount = documentRepository.countByStatus("REJECTED");

            return Map.of(
                    "success", true,
                    "stats", Map.of(
                            "total", totalDocuments,
                            "pending", pendingCount,
                            "approved", approvedCount,
                            "rejected", rejectedCount
                    )
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi lấy thống kê: " + e.getMessage());
        }
    }

    // 7. Upload tài liệu
    public Map<String, Object> uploadDocument(Long internId, String documentType, MultipartFile file, String uploaderEmail) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File không được để trống");
            }

            if (documentType == null || documentType.trim().isEmpty()) {
                throw new IllegalArgumentException("Loại tài liệu không được để trống");
            }

            // Upload to Cloudinary
            String uploadResponse = cloudinaryService.uploadFile(file);
            JsonNode json = objectMapper().readTree(uploadResponse);
            String fileUrl = json.get("secure_url").asText();

            InternDocument document = InternDocument.builder()
                    .documentName(file.getOriginalFilename())
                    .documentType(documentType)
                    .fileDetail(fileUrl)
                    .status("PENDING")
                    .uploadedAt(LocalDateTime.now())
                    .build();

            if (internId != null) {
                InternProfile internProfile = internProfileRepository.findById(internId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thực tập sinh với ID: " + internId));
                document.setInternProfile(internProfile);
            }

            InternDocument savedDocument = documentRepository.save(document);

            return Map.of(
                    "success", true,
                    "message", "Upload tài liệu thành công!",
                    "data", convertToMap(savedDocument)
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi upload tài liệu: " + e.getMessage());
        }
    }

    // 8. Duyệt tài liệu
    public Map<String, Object> approveDocument(Long id) {
        try {
            InternDocument document = documentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu với ID: " + id));

            document.setStatus("APPROVED");
            document.setReviewedAt(LocalDateTime.now());

            documentRepository.save(document);

            return Map.of(
                    "success", true,
                    "message", "Duyệt tài liệu thành công!",
                    "data", convertToMap(document)
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi duyệt tài liệu: " + e.getMessage());
        }
    }

    // 9. Từ chối tài liệu
    public Map<String, Object> rejectDocument(Long id, String reason) {
        try {
            InternDocument document = documentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu với ID: " + id));

            document.setStatus("REJECTED");
            document.setReviewedAt(LocalDateTime.now());

            documentRepository.save(document);

            return Map.of(
                    "success", true,
                    "message", "Từ chối tài liệu thành công!",
                    "reason", reason,
                    "data", convertToMap(document)
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi từ chối tài liệu: " + e.getMessage());
        }
    }

    // 10. Review tài liệu (approve/reject)
    public Map<String, Object> reviewDocument(Long id, String action, String note) {
        try {
            if (action == null || (!action.equals("approve") && !action.equals("reject"))) {
                throw new IllegalArgumentException("Action phải là 'approve' hoặc 'reject'");
            }

            if (action.equals("approve")) {
                return approveDocument(id);
            } else {
                return rejectDocument(id, note != null ? note : "Không có lý do");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Xử lý tài liệu thất bại: " + e.getMessage());
        }
    }

    // 11. Upload tài liệu cho intern
    public Map<String, Object> uploadForIntern(Long internId, String documentType, MultipartFile file) {
        return uploadDocument(internId, documentType, file, null);
    }

    // 12. Xác nhận tài liệu
    public Map<String, Object> confirmDocument(Long id) {
        try {
            InternDocument document = documentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu với ID: " + id));

            document.setStatus("CONFIRMED");
            document.setReviewedAt(LocalDateTime.now());

            documentRepository.save(document);

            return Map.of(
                    "success", true,
                    "message", "Xác nhận tài liệu thành công!",
                    "data", convertToMap(document)
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Xác nhận thất bại: " + e.getMessage());
        }
    }

    // 13. Xóa tài liệu
    public Map<String, Object> deleteDocument(Long id) {
        try {
            InternDocument document = documentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu với ID: " + id));

            documentRepository.delete(document);

            return Map.of(
                    "success", true,
                    "message", "Xóa tài liệu thành công!"
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Xóa tài liệu thất bại: " + e.getMessage());
        }
    }

    // Helper method: Convert entity to map
    private Map<String, Object> convertToMap(InternDocument document) {
        Map<String, Object> map = new HashMap<>();
        map.put("document_id", document.getId());
        map.put("document_type", document.getDocumentType());
        map.put("file_detail", document.getFileDetail());
        map.put("status", document.getStatus());
        map.put("uploaded_at", document.getUploadedAt());
        map.put("reviewed_at", document.getReviewedAt());

        if (document.getInternProfile() != null) {
            InternProfile intern = document.getInternProfile();
            map.put("intern_id", intern.getId());
            map.put("intern_name", intern.getFullName());
            if (intern.getUniversity() != null) {
                map.put("university", intern.getUniversity().getName());
            }
        }

        if (document.getReviewedBy() != null) {
            map.put("reviewed_by", document.getReviewedBy().getFullName());
        }

        return map;
    }
}
