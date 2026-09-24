package com.backend.fourth.student.dto;

public record StudentExaminationSummaryResponse(
        Integer examSessionId,
        String courseCode,
        String examDate,
        String startTime,
        String endTime,
        String academicYear,
        Integer semester,
        String examType,
        String examStatus,
        boolean allocated,
        String venueName,
        String building,
        boolean passGenerated,
        Long passId,
        String attendanceStatus
) {
}
