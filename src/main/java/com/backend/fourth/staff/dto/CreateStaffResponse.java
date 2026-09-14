package com.backend.fourth.staff.dto;

import java.time.LocalDateTime;

public record CreateStaffResponse(
        Integer staffId,
        String fullName,
        String email,
        String phone,
        String department,
        String role,
        String accountStatus,
        String activationToken,
        LocalDateTime expiresAt
) {}
