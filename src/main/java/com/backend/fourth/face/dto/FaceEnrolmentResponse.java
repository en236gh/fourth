package com.backend.fourth.face.dto;

import com.backend.fourth.face.entity.StudentFaceTemplate;

import java.time.LocalDateTime;

public record FaceEnrolmentResponse(
        String computerNumber,
        boolean enrolled,
        String modelName,
        Float detectionScore,
        LocalDateTime enrolledAt,
        LocalDateTime updatedAt
) {
    public static FaceEnrolmentResponse from(StudentFaceTemplate template) {
        return new FaceEnrolmentResponse(
                template.getComputerNumber(),
                true,
                template.getModelName(),
                template.getDetectionScore(),
                template.getEnrolledAt(),
                template.getUpdatedAt());
    }

    public static FaceEnrolmentResponse notEnrolled(String computerNumber) {
        return new FaceEnrolmentResponse(computerNumber, false, null, null, null, null);
    }
}
