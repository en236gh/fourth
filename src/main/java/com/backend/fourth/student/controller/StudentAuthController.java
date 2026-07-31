package com.backend.fourth.student.controller;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.student.dto.ActivateAccountRequest;
import com.backend.fourth.student.dto.StudentLoginRequest;
import com.backend.fourth.student.dto.StudentLoginResponse;
import com.backend.fourth.student.dto.StudentProfileResponse;
import com.backend.fourth.student.dto.StudentRefreshRequest;
import com.backend.fourth.student.service.StudentAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/auth")
@RequiredArgsConstructor
public class StudentAuthController {
    private final StudentAuthService studentAuthService;

    @PostMapping("/activate")
    public ApiResponse<StudentProfileResponse> activate(@Valid @RequestBody ActivateAccountRequest request) {
        return ApiResponse.success("Account activated successfully", studentAuthService.activate(request));
    }

    @PostMapping("/login")
    public ApiResponse<StudentLoginResponse> login(@Valid @RequestBody StudentLoginRequest request) {
        return ApiResponse.success("Authenticated", studentAuthService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<StudentLoginResponse> refresh(@Valid @RequestBody StudentRefreshRequest request) {
        return ApiResponse.success("Token refreshed", studentAuthService.refresh(request.refreshToken()));
    }
}
