package com.example.be.service;

import com.example.be.repository.InternRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InternService {
    private final InternRepository internRepository;

    public Map<String, Object> searchInterns(String query, String university, String major,
                                             String program, Integer yearOfStudy, String status,
                                             int page, int size) {
        try {
            var pageRequest = PageRequest.of(page, size);
            var interns = internRepository.searchInterns(query, university, major,
                    program, yearOfStudy, status, pageRequest);

            return Map.of(
                    "success", true,
                    "data", interns.getContent(),
                    "total", interns.getTotalElements(),
                    "totalPages", interns.getTotalPages(),
                    "currentPage", page
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "Lỗi khi tìm kiếm thực tập sinh: " + e.getMessage()
            );
        }
    }

    public Map<String, Object> getInternById(Long id) {
        try {
            var intern = internRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thực tập sinh với ID: " + id));

            return Map.of(
                    "success", true,
                    "data", intern
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy chi tiết thực tập sinh: " + e.getMessage()
            );
        }
    }

    public Map<String, Object> getInternStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", internRepository.count());

            // Convert Object[] to Map for status stats
            Map<String, Long> statusStats = new HashMap<>();
            internRepository.countByStatus().forEach(row ->
                    statusStats.put((String) row[0], (Long) row[1]));
            stats.put("byStatus", statusStats);

            // Convert Object[] to Map for university stats
            Map<String, Long> universityStats = new HashMap<>();
            internRepository.countByUniversity().forEach(row ->
                    universityStats.put((String) row[0], (Long) row[1]));
            stats.put("byUniversity", universityStats);

            // Convert Object[] to Map for program stats
            Map<String, Long> programStats = new HashMap<>();
            internRepository.countByProgram().forEach(row ->
                    programStats.put((String) row[0], (Long) row[1]));
            stats.put("byProgram", programStats);

            return Map.of(
                    "success", true,
                    "data", stats
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thống kê: " + e.getMessage()
            );
        }
    }

    public Map<String, Object> getUniversities() {
        try {
            return Map.of(
                    "success", true,
                    "data", internRepository.findDistinctUniversities()
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách trường: " + e.getMessage()
            );
        }
    }

    public Map<String, Object> getMajors() {
        try {
            return Map.of(
                    "success", true,
                    "data", internRepository.findDistinctMajors()
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách ngành: " + e.getMessage()
            );
        }
    }
}
