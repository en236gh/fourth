package com.backend.fourth.student.dto;

public record ExaminationPassExamItem(
        Integer examSessionId,
        String courseCode,
        String examDate,
        String startTime,
        String endTime,
        String examType,
        String examStatus,
        String venueName,
        String building,
        String seatNumber
) {
}
