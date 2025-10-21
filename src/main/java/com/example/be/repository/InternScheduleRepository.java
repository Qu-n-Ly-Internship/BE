package com.example.be.repository;

import com.example.be.entity.InternSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InternScheduleRepository extends JpaRepository<InternSchedule, Long> {
    List<InternSchedule> findByIntern_Id(Long internId);
    List<InternSchedule> findByIntern_IdAndDateBetween(Long internId, LocalDate start, LocalDate end);
}
