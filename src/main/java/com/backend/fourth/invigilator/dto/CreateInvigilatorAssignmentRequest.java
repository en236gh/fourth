package com.backend.fourth.invigilator.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInvigilatorAssignmentRequest(
        @NotNull Integer examSessionId,
        @NotNull Integer venueId,
        @NotNull Integer staffId,
        @Size(max = 2000) String notes) {
}