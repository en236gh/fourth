package com.backend.fourth.invigilator.dto;

import java.util.List;

public record AutoAssignmentResponse(
        Integer examSessionId,
        int createdDraftAssignments,
        List<Integer> understaffedVenueIds,
        List<AdminAssignmentResponse> assignments) {
}