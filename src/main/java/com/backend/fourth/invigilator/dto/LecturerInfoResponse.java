package com.backend.fourth.invigilator.dto;

public record LecturerInfoResponse(
        Integer staffId,
        String staffNo,
        String fullName,
        String email,
        String department
) {
}
