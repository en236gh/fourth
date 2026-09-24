package com.backend.fourth.exam.controller;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.exam.dto.ExamSessionResponse;
import com.backend.fourth.exam.dto.ExamVenueResponse;
import com.backend.fourth.exam.dto.RegisteredStudentResponse;
import com.backend.fourth.exam.service.ExamService;
import com.backend.fourth.exam.service.LecturerCourseAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {
    private final ExamService examService;
    private final LecturerCourseAccess lecturerCourseAccess;

    @GetMapping("/my-course-hierarchy")
    @PreAuthorize("hasAuthority('LECTURER')")
    public ApiResponse<List<Map<String, Object>>> myCourseHierarchy() {
        return ApiResponse.success("Assigned course hierarchy retrieved", lecturerCourseAccess.myCourseHierarchy());
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasAuthority('LECTURER')")
    public ApiResponse<List<String>> myCourses() {
        return ApiResponse.success("Assigned courses retrieved", lecturerCourseAccess.myCourseCodes());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LECTURER') or hasAuthority('ADMINISTRATOR')")
    public ApiResponse<List<ExamSessionResponse>> listExams() {
        return ApiResponse.success("Exam sessions retrieved", examService.listExams());
    }

    @GetMapping("/{examSessionId}/registered-students")
    @PreAuthorize("hasAuthority('LECTURER') or hasAuthority('ADMINISTRATOR')")
    public ApiResponse<List<RegisteredStudentResponse>> registeredStudents(@PathVariable Integer examSessionId) {
        return ApiResponse.success("Registered students retrieved", examService.listRegisteredStudents(examSessionId));
    }

    @GetMapping("/{examSessionId}/venues")
    @PreAuthorize("hasAuthority('LECTURER') or hasAuthority('ADMINISTRATOR') or hasAuthority('INVIGILATOR')")
    public ApiResponse<List<ExamVenueResponse>> venues(@PathVariable Integer examSessionId) {
        return ApiResponse.success("Exam venues retrieved", examService.listExamVenues(examSessionId));
    }
}
