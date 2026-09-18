package com.backend.fourth.invigilator.dto;

import java.time.LocalDateTime;

public record AdminAssignmentResponse(
        Integer examSessionId,
        Integer venueId,
        Integer staffId,
        String staffName,
        String assignmentStatus,
        LocalDateTime assignedAt,
        LocalDateTime publishedAt,
        String notes) {
}