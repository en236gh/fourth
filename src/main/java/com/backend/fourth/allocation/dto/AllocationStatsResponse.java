package com.backend.fourth.allocation.dto;

import java.util.List;

public record AllocationStatsResponse(
        Integer examSessionId,
        long registeredStudents,
        long allocatedStudents,
        long totalVenueCapacity,
        List<VenueFillStats> venueFills,
        List<AllocationItem> allocations
) {
    public record VenueFillStats(Integer venueId, String venueName, int capacity, long allocated) {
    }

    public record AllocationItem(String computerNumber, String studentName, Integer venueId, String venueName, String seatNumber) {
    }
}
