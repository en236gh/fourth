package com.backend.fourth.invigilator.dto;

import com.backend.fourth.exam.dto.ExamSessionResponse;
import java.util.List;

public record AssignmentCatalogResponse(
        AssignmentScope scope,
        int createdDraftAssignments,
        int totalExams,
        int totalVenues,
        long draftAssignments,
        long publishedAssignments,
        long understaffedVenues,
        List<ExamReview> exams) {

    public record AcademicHierarchy(Integer schoolId, String schoolName,
            Integer programmeId, String programmeName, Integer yearOfStudy,
            Integer semester, String courseCode, String courseName) {
        public AcademicSelection selection() {
            return new AcademicSelection(schoolId, programmeId, yearOfStudy, courseCode);
        }
    }

    public record ExamReview(ExamSessionResponse exam, List<AcademicHierarchy> hierarchy,
            List<VenueReview> venues, List<String> issues) {
    }

    public record VenueReview(Integer venueId, String venueName, String building,
            long allocatedStudentCount, int requiredInvigilatorCount,
            int missingInvigilatorCount, String staffingStatus,
            List<AdminAssignmentResponse> invigilators, List<String> issues) {
    }
}
