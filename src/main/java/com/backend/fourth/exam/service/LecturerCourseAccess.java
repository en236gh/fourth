package com.backend.fourth.exam.service;

import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.exam.entity.CourseLecturer;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.CourseLecturerRepository;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.exam.repository.LecturerHierarchyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LecturerCourseAccess {
    private final CurrentStaffResolver currentStaffResolver;
    private final CourseLecturerRepository courseLecturerRepository;
    private final ExamSessionRepository examSessionRepository;
    private final LecturerHierarchyRepository lecturerHierarchyRepository;

    public List<Map<String, Object>> myCourseHierarchy() {
        return lecturerHierarchyRepository.findByStaffId(currentStaffResolver.requireCurrentStaff().getStaffId());
    }

    public boolean hasAuthority(String authority) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    public List<String> myCourseCodes() {
        return courseLecturerRepository.findByStaffIdOrderByCourseCodeAsc(
                        currentStaffResolver.requireCurrentStaff().getStaffId()).stream()
                .map(CourseLecturer::getCourseCode).toList();
    }

    public List<ExamSession> myExams() {
        List<String> courses = myCourseCodes();
        return courses.isEmpty() ? List.of() : examSessionRepository.findByCourseCodeIn(courses);
    }

    public List<ExamSession> visibleExams() {
        return hasAuthority("ADMINISTRATOR") ? examSessionRepository.findAll() : myExams();
    }

    public void requireAssigned(ExamSession exam) {
        if (!courseLecturerRepository.existsByCourseCodeAndStaffId(exam.getCourseCode(),
                currentStaffResolver.requireCurrentStaff().getStaffId())) {
            throw new AccessDeniedException("You are not assigned to this examination's course");
        }
    }

    public void requireReadAccess(ExamSession exam) {
        if (!hasAuthority("ADMINISTRATOR")) {
            requireAssigned(exam);
        }
    }
}
