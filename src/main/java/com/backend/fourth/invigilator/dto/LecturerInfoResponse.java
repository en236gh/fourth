package com.backend.fourth.invigilator.dto;

public record LecturerInfoResponse(
        Integer staffId,
        String fullName,
        String email,
        String department
) {
}
