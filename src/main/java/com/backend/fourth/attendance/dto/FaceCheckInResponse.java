package com.backend.fourth.attendance.dto;

public record FaceCheckInResponse(
        Outcome outcome,
        String computerNumber,
        Double similarity,
        double matchThreshold,
        double reviewThreshold,
        AttendanceCheckInResponse attendance,
        Integer incidentId,
        String message
) {
    public enum Outcome {
        /** Similarity reached the match threshold; attendance recorded. */
        VERIFIED,
        /** Grey-zone similarity; the invigilator supplied an override reason and attendance was recorded. */
        CONFIRMED_BY_INVIGILATOR,
        /** Grey-zone similarity; nothing recorded. Retake the photo or resend with an override reason. */
        REVIEW_REQUIRED,
        /** Clear mismatch; nothing recorded and an IMPERSONATION incident was raised. */
        REJECTED,
        /** The student has no enrolled face; nothing recorded. Use QR check-in instead. */
        NOT_ENROLLED
    }
}
