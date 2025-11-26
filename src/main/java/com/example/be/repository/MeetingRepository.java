package com.example.be.repository;

import com.example.be.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    // Lấy tất cả meetings của 1 intern
    List<Meeting> findByIntern_Id(Long internId);

    // Lấy meetings theo HR đã tạo
    List<Meeting> findByCreatedBy_Id(Long hrId);

    // Lấy meetings theo status
    List<Meeting> findByStatus(String status);

    // Lấy meetings trong khoảng thời gian
    @Query("SELECT m FROM Meeting m WHERE m.startTime >= :startTime AND m.endTime <= :endTime")
    List<Meeting> findMeetingsInRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Lấy upcoming meetings của intern
    @Query("SELECT m FROM Meeting m WHERE m.intern.id = :internId AND m.startTime > :now AND m.status = 'SCHEDULED' ORDER BY m.startTime ASC")
    List<Meeting> findUpcomingMeetingsByIntern(@Param("internId") Long internId, @Param("now") LocalDateTime now);
}