package com.backend.fourth.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CheckInRequest(
        @NotBlank
        @Pattern(regexp = "^\\d{4}\\d{6}$", message = "Computer number must be 10 digits and start with a four-digit year")
        String computerNumber,
        @NotNull Integer examSessionId,
        @NotNull Integer venueId,
        @NotBlank String verificationMethod
) {
}
