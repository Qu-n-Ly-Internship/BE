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

    // ✅ THAY ĐỔI: Lấy meetings theo program_id thay vì intern_id
    List<Meeting> findByProgram_Id(Long programId);

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

    // ✅ Lấy upcoming meetings của một program
    @Query("SELECT m FROM Meeting m WHERE m.program.id = :programId AND m.startTime > :now AND m.status = 'SCHEDULED' ORDER BY m.startTime ASC")
    List<Meeting> findUpcomingMeetingsByProgram(@Param("programId") Long programId, @Param("now") LocalDateTime now);

    // ✅ Đếm số meetings của một program
    @Query("SELECT COUNT(m) FROM Meeting m WHERE m.program.id = :programId")
    long countByProgramId(@Param("programId") Long programId);
}