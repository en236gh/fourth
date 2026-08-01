package com.backend.fourth.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QrCheckInRequest(
        @NotBlank String qrToken,
        @NotNull Integer examSessionId,
        @NotNull Integer venueId
) {
}
