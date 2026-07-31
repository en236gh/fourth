package com.backend.fourth.attendance.dto;

public record AttendanceSummaryResponse(
        long checkedIn,
        long absent,
        long scriptsCollected,
        long incidents
) {
}
