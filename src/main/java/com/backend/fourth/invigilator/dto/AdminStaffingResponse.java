package com.backend.fourth.invigilator.dto;

import java.util.List;

public record AdminStaffingResponse(
        Integer examSessionId,
        Integer venueId,
        long allocatedStudentCount,
        int requiredInvigilatorCount,
        long assignedInvigilatorCount,
        long draftInvigilatorCount,
        long publishedInvigilatorCount,
        String staffingStatus,
        List<AdminStaffMemberResponse> assignedInvigilators,
        List<AdminStaffMemberResponse> remainingInvigilators,
        long totalActiveInvigilatorCount,
        long totalAssignedInvigilatorCount,
        long totalRemainingInvigilatorCount) {
}