package com.backend.fourth.student.dto;

import jakarta.validation.constraints.NotBlank;

public record StudentRefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
