package com.backend.fourth.exam.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ExamSessionResponse(
        Integer examSessionId,
        String courseCode,
        LocalDate examDate,
        LocalTime startTime,
        LocalTime endTime,
        String academicYear,
        Integer semester,
        String examType,
        String status
) {
}
