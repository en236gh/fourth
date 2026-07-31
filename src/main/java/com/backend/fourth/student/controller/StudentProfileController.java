package com.backend.fourth.student.controller;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.common.security.CurrentStudentResolver;
import com.backend.fourth.student.dto.StudentProfileResponse;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.service.StudentAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentProfileController {
    private final StudentAuthService studentAuthService;
    private final CurrentStudentResolver currentStudentResolver;

    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ApiResponse<StudentProfileResponse> profile() {
        Student student = currentStudentResolver.requireCurrentStudent();
        return ApiResponse.success(
                "Profile retrieved",
                studentAuthService.getProfile(student.getComputerNumber()));
    }
}
