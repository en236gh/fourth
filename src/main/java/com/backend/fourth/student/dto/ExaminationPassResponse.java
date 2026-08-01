package com.backend.fourth.student.dto;

import java.util.List;

public record ExaminationPassResponse(
        Long passId,
        String academicYear,
        Integer semester,
        String computerNumber,
        String fullName,
        String school,
        String programme,
        Integer currentYear,
        String qrToken,
        String qrImageBase64,
        String generatedAt,
        String expiresAt,
        List<ExaminationPassExamItem> examinations
) {
}
