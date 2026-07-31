package com.backend.fourth.student.dto;

public record StudentLoginResponse(
        String accessToken,
        String refreshToken,
        StudentProfileResponse profile
) {
}
