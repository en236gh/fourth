package com.backend.fourth.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StudentLoginRequest(
        @NotBlank
        @Pattern(regexp = "^\\d{4}\\d{6}$", message = "Computer number must be 10 digits and start with a four-digit year")
        String computerNumber,

        @NotBlank(message = "Password is required")
        String password
) {
}
