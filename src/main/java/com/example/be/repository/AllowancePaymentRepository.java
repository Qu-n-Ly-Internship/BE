package com.example.be.repository;

import com.example.be.entity.AllowancePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface AllowancePaymentRepository extends JpaRepository<AllowancePayment, Long> {
    List<AllowancePayment> findByIntern_Id(Long internId);

    @Query("SELECT a FROM AllowancePayment a WHERE a.intern.id = :internId AND a.date BETWEEN :startDate AND :endDate")
    List<AllowancePayment> findByInternIdAndDateRange(
            @Param("internId") Long internId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM AllowancePayment a WHERE a.intern.id = :internId")
    Double getTotalAllowanceByInternId(@Param("internId") Long internId);
}