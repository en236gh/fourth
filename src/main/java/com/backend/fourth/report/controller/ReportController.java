package com.backend.fourth.report.controller;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.invigilator.repository.InvigilatorAssignmentRepository;
import com.backend.fourth.report.entity.GeneratedReport;
import com.backend.fourth.report.repository.GeneratedReportRepository;
import com.backend.fourth.report.service.ReportService;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final ExamSessionRepository examSessionRepository;
    private final VenueRepository venueRepository;
    private final GeneratedReportRepository generatedReportRepository;
    private final InvigilatorAssignmentRepository assignmentRepository;
    private final CurrentStaffResolver currentStaffResolver;

    @PostMapping("/exam-session/{examSessionId}")
    @PreAuthorize("hasAuthority('INVIGILATOR')")
    public ApiResponse<GeneratedReport> generateReport(@PathVariable Integer examSessionId) throws Exception {
        Staff staff = currentStaffResolver.requireCurrentStaff();
        ExamSession examSession = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));

        Integer venueId = assignmentRepository.findByStaffIdAndExamSessionId(staff.getStaffId(), examSessionId).stream()
                .findFirst()
                .map(assignment -> assignment.getVenueId())
                .orElseThrow(() -> new IllegalArgumentException("You are not assigned to this examination"));
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        return ApiResponse.success("Report generated", reportService.generateExamReport(examSession, staff, venue));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<java.util.List<GeneratedReport>> listReports() {
        return ApiResponse.success("Reports retrieved", generatedReportRepository.findAll());
    }
}
