package com.backend.fourth.exam.dto;

public record ExamVenueResponse(
        Integer venueId,
        String venueName,
        String building,
        Integer capacity
) {
}
