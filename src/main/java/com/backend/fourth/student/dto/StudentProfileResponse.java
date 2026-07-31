package com.backend.fourth.student.dto;

public record StudentProfileResponse(
        String computerNumber,
        String fullName,
        String school,
        String programme,
        Integer currentYear,
        String accountStatus,
        boolean accountActivated
) {
}
