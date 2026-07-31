package com.backend.fourth.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIncidentRequest(
        @NotNull Integer examSessionId,
        @NotNull Integer venueId,
        String computerNumber,
        @NotBlank String incidentType,
        @NotBlank String description,
        String severity,
        String evidencePath
) {
}
