package com.backend.fourth.student.dto;

public record ExaminationSlipResponse(
        Long slipId,
        Integer examSessionId,
        String courseCode,
        String examDate,
        String startTime,
        String endTime,
        String academicYear,
        Integer semester,
        String examType,
        String examStatus,
        String computerNumber,
        String fullName,
        String school,
        String programme,
        Integer currentYear,
        String venueName,
        String building,
        String seatNumber,
        String qrToken,
        String qrImageBase64,
        String generatedAt
) {
}
