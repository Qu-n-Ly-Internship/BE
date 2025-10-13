package com.example.be.repository;

import com.example.be.entity.CV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CVRepository extends JpaRepository<CV, Long> {

    // Lấy CV theo intern_id
    List<CV> findByInternProfileId(Long internId);

    // Lấy CV theo trạng thái
    List<CV> findByStatus(String status);

    // Lấy CV theo intern và trạng thái
    List<CV> findByInternProfileIdAndStatus(Long internId, String status);

    // Query tùy chỉnh để lấy CV kèm thông tin intern
    @Query("SELECT c FROM CV c " +
            "JOIN FETCH c.internProfile ip " +
            "LEFT JOIN FETCH ip.university " +
            "LEFT JOIN FETCH ip.program " +
            "WHERE (:status IS NULL OR c.status = :status)")
    List<CV> findCVsWithFilters(@Param("status") String status);

    // Đếm số CV theo trạng thái
    long countByStatus(String status);

    // Lấy CV chờ duyệt (PENDING)
    @Query("SELECT c FROM CV c " +
            "JOIN FETCH c.internProfile ip " +
            "WHERE c.status = 'PENDING' " +
            "ORDER BY c.uploadedBy ASC")
    List<CV> findPendingCVs();
}
