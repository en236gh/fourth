package com.backend.fourth.exam.repository;

import com.backend.fourth.exam.entity.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExamSessionRepository extends JpaRepository<ExamSession, Integer> {
    List<ExamSession> findByExamDate(LocalDate examDate);

    List<ExamSession> findByStatus(String status);

    List<ExamSession> findByCourseCodeAndAcademicYearAndSemester(
            String courseCode, String academicYear, Integer semester);

    long countByExamDate(LocalDate examDate);
}
