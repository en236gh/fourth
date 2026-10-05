package com.backend.fourth.incident.dto;

import java.time.LocalDateTime;

public record IncidentEvidenceResponse(
        Integer evidenceId,
        Integer incidentId,
        String bucket,
        String objectPath,
        String originalFilename,
        String mimeType,
        Long sizeBytes,
        Integer uploadedByStaffId,
        LocalDateTime uploadedAt
) {
}