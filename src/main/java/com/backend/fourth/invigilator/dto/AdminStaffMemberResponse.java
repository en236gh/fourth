package com.backend.fourth.invigilator.dto;

public record AdminStaffMemberResponse(
        Integer staffId,
        String staffName,
        String assignmentStatus) {
}