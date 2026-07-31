package com.backend.fourth.invigilator.dto;

import java.util.List;

public record InvigilatorAssignmentResponse(
        Integer examSessionId,
        String courseCode,
        String examDate,
        String startTime,
        String endTime,
        String examStatus,
        Integer venueId,
        String venueName,
        String building,
        Integer capacity,
        List<LecturerInfoResponse> lecturers
) {
}
