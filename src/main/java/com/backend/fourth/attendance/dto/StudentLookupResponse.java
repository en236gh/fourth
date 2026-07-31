package com.backend.fourth.attendance.dto;

public record StudentLookupResponse(
        String computerNumber,
        String fullName,
        String program,
        String photoPath,
        Integer allocatedVenueId,
        String allocatedVenueName,
        String seatNumber,
        boolean alreadyCheckedIn
) {
}
