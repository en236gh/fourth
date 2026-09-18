package com.backend.fourth.invigilator.dto;

public record AdminStaffingResponse(
        Integer examSessionId,
        Integer venueId,
        long allocatedStudentCount,
        int requiredInvigilatorCount,
        long assignedInvigilatorCount,
        long draftInvigilatorCount,
        long publishedInvigilatorCount,
        String staffingStatus) {
}