package com.backend.fourth.invigilator.dto;

import java.util.List;

public record BulkAutoAssignmentResponse(int totalExams, int createdDraftAssignments,
        List<Integer> examsWithoutVenues, List<AutoAssignmentResponse> exams) {
}
