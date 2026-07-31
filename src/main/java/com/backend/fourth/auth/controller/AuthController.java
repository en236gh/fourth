package com.backend.fourth.auth.controller;

import com.backend.fourth.auth.dto.LoginRequest;
import com.backend.fourth.auth.dto.RefreshRequest;
import com.backend.fourth.auth.dto.TokenResponse;
import com.backend.fourth.auth.entity.RefreshToken;
import com.backend.fourth.auth.repository.RefreshTokenRepository;
import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.security.JwtService;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.staff.repository.StaffRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        Staff staff = staffRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), staff.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return ApiResponse.success("Authenticated", issueTokens(staff));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        if (!jwtService.isTokenValid(request.refreshToken())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new IllegalArgumentException("Invalid refresh token");
        }

        Staff staff = stored.getStaff();
        String username = jwtService.extractUsername(request.refreshToken());
        if (staff == null || staff.getEmail() == null || !staff.getEmail().equalsIgnoreCase(username)) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // Rotate: revoke the used refresh token, then issue a fresh pair.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return ApiResponse.success("Token refreshed", issueTokens(staff));
    }

    private TokenResponse issueTokens(Staff staff) {
        List<String> roles = staff.getRoles().stream().map(role -> role.getName()).toList();
        String accessToken = jwtService.generateAccessToken(staff.getEmail(), roles);
        String refreshToken = jwtService.generateRefreshToken(staff.getEmail());

        RefreshToken entity = new RefreshToken();
        entity.setStaff(staff);
        entity.setToken(refreshToken);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusDays(7));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);

        return new TokenResponse(accessToken, refreshToken);
    }
}
