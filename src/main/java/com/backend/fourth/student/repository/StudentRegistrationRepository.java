package com.backend.fourth.student.repository;

import com.backend.fourth.student.entity.StudentRegistration;
import com.backend.fourth.student.entity.StudentRegistrationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRegistrationRepository extends JpaRepository<StudentRegistration, StudentRegistrationId> {
    List<StudentRegistration> findByCourseCodeAndAcademicYearAndSemesterOrderByComputerNumberAsc(
            String courseCode, String academicYear, Integer semester);

    List<StudentRegistration> findByComputerNumber(String computerNumber);

    boolean existsByComputerNumberAndCourseCodeAndAcademicYearAndSemester(
            String computerNumber, String courseCode, String academicYear, Integer semester);

    long countByCourseCodeAndAcademicYearAndSemester(String courseCode, String academicYear, Integer semester);
}
