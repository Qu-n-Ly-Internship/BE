package com.example.be.repository;

import com.example.be.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // Lấy theo intern_id
    List<LeaveRequest> findByIntern_Id(Long internId);

    // Lấy theo status
    List<LeaveRequest> findByStatus(String status);

    // Lấy theo intern và status
    List<LeaveRequest> findByIntern_IdAndStatus(Long internId, String status);

    // Lấy theo khoảng thời gian
    @Query("SELECT l FROM LeaveRequest l WHERE l.startDate >= :startDate AND l.endDate <= :endDate")
    List<LeaveRequest> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Đếm theo status
    long countByStatus(String status);

    // Lấy danh sách chờ duyệt
    @Query("SELECT l FROM LeaveRequest l WHERE l.status = 'PENDING' ORDER BY l.createdAt ASC")
    List<LeaveRequest> findPendingRequests();
}