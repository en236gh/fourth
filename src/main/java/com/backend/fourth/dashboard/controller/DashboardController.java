package com.backend.fourth.dashboard.controller;

import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.allocation.service.AllocationService;
import com.backend.fourth.attendance.entity.AttendanceStatus;
import com.backend.fourth.attendance.repository.AttendanceRepository;
import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.entity.ExamVenue;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.exam.repository.ExamVenueRepository;
import com.backend.fourth.incident.repository.IncidentRepository;
import com.backend.fourth.invigilator.entity.InvigilatorAssignment;
import com.backend.fourth.invigilator.repository.InvigilatorAssignmentRepository;
import com.backend.fourth.report.repository.GeneratedReportRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.student.repository.StudentRegistrationRepository;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final ExamSessionRepository examSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final GeneratedReportRepository generatedReportRepository;
    private final StudentVenueAllocationRepository allocationRepository;
    private final StudentRegistrationRepository registrationRepository;
    private final ExamVenueRepository examVenueRepository;
    private final VenueRepository venueRepository;
    private final IncidentRepository incidentRepository;
    private final InvigilatorAssignmentRepository assignmentRepository;
    private final AllocationService allocationService;
    private final CurrentStaffResolver currentStaffResolver;

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<Map<String, Object>> adminDashboard() {
        LocalDate today = LocalDate.now();
        List<ExamSession> todaysExams = examSessionRepository.findByExamDate(today);
        List<Integer> todayExamIds = todaysExams.stream()
                .map(ExamSession::getExamSessionId)
                .toList();

        long present;
        long absent;
        if (todayExamIds.isEmpty()) {
            present = attendanceRepository.countByAttendanceStatus(AttendanceStatus.PRESENT);
            absent = attendanceRepository.countByAttendanceStatus(AttendanceStatus.ABSENT);
        } else {
            present = todayExamIds.stream()
                    .mapToLong(id -> attendanceRepository.countByExamSessionExamSessionIdAndAttendanceStatus(
                            id, AttendanceStatus.PRESENT))
                    .sum();
            absent = todayExamIds.stream()
                    .mapToLong(id -> attendanceRepository.countByExamSessionExamSessionIdAndAttendanceStatus(
                            id, AttendanceStatus.ABSENT))
                    .sum();
        }

        long denominator = present + absent;
        double attendancePercentage = denominator == 0 ? 0.0 : (present * 100.0) / denominator;

        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);
        long incidentsToday = incidentRepository.countByOccurredAtBetween(dayStart, dayEnd);

        List<Map<String, Object>> venueOccupancy = buildVenueOccupancy(todaysExams);

        Map<String, Object> data = new HashMap<>();
        data.put("todaysExaminations", todaysExams.size());
        data.put("todaysExaminationDetails", todaysExams.stream().map(exam -> Map.of(
                "examSessionId", exam.getExamSessionId(),
                "courseCode", exam.getCourseCode(),
                "startTime", exam.getStartTime().toString(),
                "endTime", exam.getEndTime().toString(),
                "status", exam.getStatus()
        )).toList());
        data.put("presentStudents", present);
        data.put("absentStudents", absent);
        data.put("attendancePercentage", Math.round(attendancePercentage * 100.0) / 100.0);
        data.put("totalIncidents", incidentRepository.count());
        data.put("incidentsToday", incidentsToday);
        data.put("venueOccupancy", venueOccupancy);
        data.put("generatedReports", generatedReportRepository.count());
        return ApiResponse.success("Admin dashboard", data);
    }

    @GetMapping("/lecturer")
    @PreAuthorize("hasAuthority('LECTURER')")
    public ApiResponse<Map<String, Object>> lecturerDashboard(
            @RequestParam(required = false) Integer examSessionId) {
        Map<String, Object> data = new HashMap<>();
        if (examSessionId != null) {
            ExamSession exam = examSessionRepository.findById(examSessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
            data.put("examSessionId", examSessionId);
            data.put("allocation", allocationService.getAllocationStats(exam));
            return ApiResponse.success("Lecturer dashboard", data);
        }

        long totalRegistered = registrationRepository.count();
        long totalAllocated = allocationRepository.count();
        data.put("totalExaminations", examSessionRepository.count());
        data.put("registeredStudents", totalRegistered);
        data.put("allocatedStudents", totalAllocated);
        data.put("message", "Pass examSessionId to view venue allocation statistics for a specific examination");
        return ApiResponse.success("Lecturer dashboard", data);
    }

    @GetMapping("/invigilator")
    @PreAuthorize("hasAuthority('INVIGILATOR')")
    public ApiResponse<Map<String, Object>> invigilatorDashboard() {
        Staff staff = currentStaffResolver.requireCurrentStaff();
        List<InvigilatorAssignment> assignments = assignmentRepository.findByStaffId(staff.getStaffId());
        List<Integer> examIds = assignments.stream()
                .map(InvigilatorAssignment::getExamSessionId)
                .distinct()
                .toList();

        long present = examIds.stream()
                .mapToLong(id -> attendanceRepository.countByExamSessionExamSessionIdAndAttendanceStatus(
                        id, AttendanceStatus.PRESENT))
                .sum();
        long absent = examIds.stream()
                .mapToLong(id -> attendanceRepository.countByExamSessionExamSessionIdAndAttendanceStatus(
                        id, AttendanceStatus.ABSENT))
                .sum();
        long scripts = examIds.isEmpty() ? 0 : attendanceRepository.findAll().stream()
                .filter(attendance -> attendance.getExamSession() != null
                        && examIds.contains(attendance.getExamSession().getExamSessionId())
                        && Boolean.TRUE.equals(attendance.getScriptsSubmitted()))
                .count();
        long incidents = examIds.isEmpty() ? 0 : incidentRepository.countByExamSessionExamSessionIdIn(examIds);

        Map<String, Object> data = new HashMap<>();
        data.put("assignedExaminations", examIds.size());
        data.put("assignedVenues", assignments.size());
        data.put("checkedInStudents", present);
        data.put("absentStudents", absent);
        data.put("scriptsCollected", scripts);
        data.put("incidents", incidents);
        return ApiResponse.success("Invigilator dashboard", data);
    }

    private List<Map<String, Object>> buildVenueOccupancy(List<ExamSession> todaysExams) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExamSession exam : todaysExams) {
            for (ExamVenue examVenue : examVenueRepository.findByExamSessionIdOrderByVenueIdAsc(exam.getExamSessionId())) {
                Venue venue = venueRepository.findById(examVenue.getVenueId()).orElse(null);
                if (venue == null) {
                    continue;
                }
                long allocated = allocationRepository.countByVenueIdAndExamSessionId(
                        venue.getVenueId(), exam.getExamSessionId());
                long checkedIn = attendanceRepository.findByExamSessionExamSessionId(exam.getExamSessionId()).stream()
                        .filter(attendance -> attendance.getVenue() != null
                                && venue.getVenueId().equals(attendance.getVenue().getVenueId())
                                && attendance.getAttendanceStatus() == AttendanceStatus.PRESENT)
                        .count();
                Map<String, Object> row = new HashMap<>();
                row.put("examSessionId", exam.getExamSessionId());
                row.put("courseCode", exam.getCourseCode());
                row.put("venueId", venue.getVenueId());
                row.put("venueName", venue.getVenueName());
                row.put("capacity", venue.getCapacity());
                row.put("allocated", allocated);
                row.put("checkedIn", checkedIn);
                row.put("occupancyPercentage", venue.getCapacity() == 0
                        ? 0.0
                        : Math.round((checkedIn * 10000.0) / venue.getCapacity()) / 100.0);
                rows.add(row);
            }
        }
        return rows;
    }
}
