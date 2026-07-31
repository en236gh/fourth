package com.backend.fourth.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActivateAccountRequest(
        @NotBlank
        @Pattern(regexp = "^\\d{4}\\d{6}$", message = "Computer number must be 10 digits and start with a four-digit year")
        String computerNumber,

        @NotBlank(message = "National ID (NRC) is required")
        String nationalId,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword
) {
}
