package com.backend.fourth.student.repository;

import com.backend.fourth.student.entity.ExaminationSlip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExaminationSlipRepository extends JpaRepository<ExaminationSlip, Long> {
    Optional<ExaminationSlip> findByStudentComputerNumberAndExamSessionId(String computerNumber, Integer examSessionId);

    List<ExaminationSlip> findByStudentComputerNumberOrderByGeneratedAtDesc(String computerNumber);

    Optional<ExaminationSlip> findByQrJti(String qrJti);
}
