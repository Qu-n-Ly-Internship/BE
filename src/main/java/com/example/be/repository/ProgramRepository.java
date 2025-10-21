package com.example.be.repository;

import com.example.be.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgramRepository extends JpaRepository<Program, Long> {

    @Query("SELECT p.hr.id FROM Program p WHERE p.id = :programId")
    Long findHrIdByProgramId(@Param("programId") Long programId);
}
