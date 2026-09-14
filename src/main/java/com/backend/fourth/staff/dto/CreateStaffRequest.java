package com.backend.fourth.staff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateStaffRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        @NotBlank(message = "Phone is required") String phone,
        @NotBlank(message = "Department is required") String department,
        @NotBlank(message = "Role is required") String role
) {}
