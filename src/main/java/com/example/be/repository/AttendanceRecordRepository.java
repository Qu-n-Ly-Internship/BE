package com.example.be.repository;

import com.example.be.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.intern.id = :internId AND ar.workDate = :date")
    Optional<AttendanceRecord> findByInternIdAndWorkDate(@Param("internId") Long internId, @Param("date") LocalDate date);

    List<AttendanceRecord> findAllByWorkDate(LocalDate date);
    List<AttendanceRecord> findByInternId(Long internId);
}


