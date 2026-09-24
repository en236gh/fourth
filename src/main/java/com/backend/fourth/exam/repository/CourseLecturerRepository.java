package com.backend.fourth.exam.repository;

import com.backend.fourth.exam.entity.CourseLecturer;
import com.backend.fourth.exam.entity.CourseLecturerId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseLecturerRepository extends JpaRepository<CourseLecturer, CourseLecturerId> {
    List<CourseLecturer> findByStaffIdOrderByCourseCodeAsc(Integer staffId);
    List<CourseLecturer> findByCourseCodeOrderByStaffIdAsc(String courseCode);

    boolean existsByCourseCodeAndStaffId(String courseCode, Integer staffId);
}
