package com.backend.fourth.auth.dto;

public record TokenResponse(String accessToken, String refreshToken) {
}
