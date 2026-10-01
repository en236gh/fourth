package com.backend.fourth.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/** multipart/form-data body of POST /api/attendance/check-in-by-qr-and-face. */
public record FaceCheckInForm(
        @NotBlank String qrToken,
        @NotNull Integer examSessionId,
        @NotNull Integer venueId,
        @NotNull MultipartFile image,
        @Size(max = 300) String overrideReason
) {
}
