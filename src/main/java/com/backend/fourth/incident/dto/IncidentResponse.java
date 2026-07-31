package com.backend.fourth.incident.dto;

import java.time.LocalDateTime;

public record IncidentResponse(
        Integer incidentId,
        Integer examSessionId,
        Integer venueId,
        String computerNumber,
        String studentName,
        String incidentType,
        String description,
        String severity,
        String evidencePath,
        Integer reportedByStaffId,
        String reportedByName,
        LocalDateTime occurredAt
) {
}
