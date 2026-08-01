package com.backend.fourth.student.repository;

import com.backend.fourth.student.entity.ExaminationPass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExaminationPassRepository extends JpaRepository<ExaminationPass, Long> {
    Optional<ExaminationPass> findByStudentComputerNumberAndAcademicYearAndSemester(
            String computerNumber, String academicYear, Integer semester);

    List<ExaminationPass> findByStudentComputerNumber(String computerNumber);

    Optional<ExaminationPass> findByQrJti(String qrJti);
}
