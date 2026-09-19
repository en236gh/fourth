package com.backend.fourth.invigilator.service;

import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.entity.ExamVenue;
import com.backend.fourth.exam.entity.ExamVenueId;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.exam.repository.ExamVenueRepository;
import com.backend.fourth.invigilator.dto.AdminAssignmentResponse;
import com.backend.fourth.invigilator.dto.AdminStaffMemberResponse;
import com.backend.fourth.invigilator.dto.AdminStaffingResponse;
import com.backend.fourth.invigilator.dto.AutoAssignmentResponse;
import com.backend.fourth.invigilator.dto.CreateInvigilatorAssignmentRequest;
import com.backend.fourth.invigilator.entity.InvigilatorAssignment;
import com.backend.fourth.invigilator.entity.InvigilatorAssignmentId;
import com.backend.fourth.invigilator.repository.InvigilatorAssignmentRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminInvigilatorAssignmentService {
    private static final int STUDENTS_PER_INVIGILATOR = 50;

    private final InvigilatorAssignmentRepository assignmentRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamVenueRepository examVenueRepository;
    private final StudentVenueAllocationRepository allocationRepository;
    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public List<AdminAssignmentResponse> list(Integer examSessionId) {
        List<InvigilatorAssignment> assignments = examSessionId == null
                ? assignmentRepository.findAll()
                : assignmentRepository.findByExamSessionId(examSessionId);
        return assignments.stream()
                .map(assignment -> toResponse(assignment, staffRepository.findById(assignment.getStaffId()).orElseThrow()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminStaffingResponse> staffing(Integer examSessionId) {
        ExamSession exam = requireAssignableExam(examSessionId);
        List<Staff> activeInvigilators = staffRepository.findAll().stream()
            .filter(this::isActiveInvigilator)
            .toList();
        List<InvigilatorAssignment> examAssignments = assignmentRepository.findByExamSessionId(examSessionId);
        return examVenueRepository.findByExamSessionIdOrderByVenueIdAsc(examSessionId).stream()
            .map(venue -> staffingFor(examSessionId, venue.getVenueId(), exam,
                activeInvigilators, examAssignments))
                .toList();
    }

    @Transactional
    public AdminAssignmentResponse createDraft(CreateInvigilatorAssignmentRequest request, Staff administrator) {
        ExamSession exam = requireAssignableExam(request.examSessionId());
        if (!examVenueRepository.existsById(new ExamVenueId(request.examSessionId(), request.venueId()))) {
            throw new IllegalArgumentException("Venue is not linked to this examination");
        }
        Staff invigilator = requireActiveInvigilator(request.staffId());
        if (assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffId(
                request.examSessionId(), request.venueId(), request.staffId())) {
            throw new IllegalStateException("This invigilator is already assigned to the examination venue");
        }
        if (hasConflict(invigilator.getStaffId(), exam, null)) {
            throw new IllegalStateException("Invigilator already has an overlapping examination assignment");
        }
        InvigilatorAssignment assignment = new InvigilatorAssignment();
        assignment.setExamSessionId(request.examSessionId());
        assignment.setVenueId(request.venueId());
        assignment.setStaffId(request.staffId());
        assignment.setAssignmentStatus("DRAFT");
        assignment.setAssignedByStaffId(administrator.getStaffId());
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setAssignmentNotes(request.notes());
        return toResponse(assignmentRepository.save(assignment), invigilator);
    }

    @Transactional
    public AutoAssignmentResponse autoAssignDrafts(Integer examSessionId, Staff administrator) {
        ExamSession exam = requireAssignableExam(examSessionId);
        List<Staff> eligible = staffRepository.findAll().stream()
                .filter(this::isActiveInvigilator)
                .toList();
        if (eligible.isEmpty()) {
            throw new IllegalStateException("No active invigilators are available");
        }
        Map<Integer, Long> workload = eligible.stream()
            .collect(Collectors.toMap(staff -> staff.getStaffId(), staff -> activeWorkload(staff.getStaffId())));

        List<AdminAssignmentResponse> created = new ArrayList<>();
        List<Integer> understaffed = new ArrayList<>();
        for (ExamVenue venue : examVenueRepository.findByExamSessionIdOrderByVenueIdAsc(examSessionId)) {
            int required = requiredInvigilators(examSessionId, venue.getVenueId());
            List<InvigilatorAssignment> existing = new ArrayList<>(assignmentRepository
                    .findByExamSessionIdAndVenueId(examSessionId, venue.getVenueId()).stream()
                    .filter(a -> !"CANCELLED".equals(a.getAssignmentStatus()))
                    .toList());
            int needed = Math.max(0, required - existing.size());
            for (int i = 0; i < needed; i++) {
                Staff selected = eligible.stream()
                        .filter(staff -> existing.stream().noneMatch(a -> a.getStaffId().equals(staff.getStaffId())))
                        .filter(staff -> !hasConflict(staff.getStaffId(), exam, null))
                        .min(Comparator.comparingLong((Staff staff) -> workload.get(staff.getStaffId()))
                            .thenComparing(staff -> staff.getStaffId()))
                        .orElse(null);
                if (selected == null) {
                    break;
                }
                InvigilatorAssignment assignment = new InvigilatorAssignment();
                assignment.setExamSessionId(examSessionId);
                assignment.setVenueId(venue.getVenueId());
                assignment.setStaffId(selected.getStaffId());
                assignment.setAssignmentStatus("DRAFT");
                assignment.setAssignedByStaffId(administrator.getStaffId());
                assignment.setAssignedAt(LocalDateTime.now());
                assignment.setAssignmentNotes("Generated automatically; administrator review required.");
                assignment = assignmentRepository.save(assignment);
                existing.add(assignment);
                workload.computeIfPresent(selected.getStaffId(), (staffId, count) -> count + 1);
                created.add(toResponse(assignment, selected));
            }
            if (existing.size() < required) {
                understaffed.add(venue.getVenueId());
            }
        }
        return new AutoAssignmentResponse(examSessionId, created.size(), understaffed, created);
    }

    @Transactional
    public List<AdminAssignmentResponse> publish(Integer examSessionId) {
        requireAssignableExam(examSessionId);
        List<InvigilatorAssignment> assignments = assignmentRepository.findByExamSessionId(examSessionId);
        if (assignments.isEmpty()) {
            throw new IllegalStateException("No assignments exist for this examination");
        }
        LocalDateTime now = LocalDateTime.now();
        return assignments.stream().filter(a -> "DRAFT".equals(a.getAssignmentStatus())).map(a -> {
            a.setAssignmentStatus("PUBLISHED");
            a.setPublishedAt(now);
            return toResponse(assignmentRepository.save(a), staffRepository.findById(a.getStaffId()).orElseThrow());
        }).toList();
    }

    @Transactional
    public AdminAssignmentResponse cancel(Integer examSessionId, Integer venueId, Integer staffId) {
        requireAssignableExam(examSessionId);
        InvigilatorAssignment assignment = assignmentRepository.findById(
                new InvigilatorAssignmentId(examSessionId, venueId, staffId))
                .orElseThrow(() -> new IllegalArgumentException("Invigilator assignment not found"));
        assignmentRepository.deleteById(new InvigilatorAssignmentId(examSessionId, venueId, staffId));
        return toResponse(assignment, staffRepository.findById(staffId).orElseThrow());
    }

    private ExamSession requireAssignableExam(Integer id) {
        ExamSession exam = examSessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
        if ("COMPLETED".equals(exam.getStatus())) {
            throw new IllegalStateException("Examination has already been completed");
        }
        return exam;
    }

    private Staff requireActiveInvigilator(Integer id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found"));
        if (!isActiveInvigilator(staff)) {
            throw new IllegalArgumentException("Staff member is not an active invigilator");
        }
        return staff;
    }

    private boolean isActiveInvigilator(Staff staff) {
        return "ACTIVE".equals(staff.getAccountStatus())
            && staff.getRoles().stream().map(role -> role.getName()).anyMatch("INVIGILATOR"::equals);
    }

    private long activeWorkload(Integer staffId) {
        return assignmentRepository.findByStaffId(staffId).stream()
                .filter(a -> !"CANCELLED".equals(a.getAssignmentStatus()))
                .count();
    }

    private boolean hasConflict(Integer staffId, ExamSession target, Integer ignoredSessionId) {
        return assignmentRepository.findByStaffId(staffId).stream().anyMatch(a -> {
            if ("CANCELLED".equals(a.getAssignmentStatus())
                    || a.getExamSessionId().equals(ignoredSessionId)) {
                return false;
            }
            ExamSession other = examSessionRepository.findById(a.getExamSessionId()).orElse(null);
            return other != null && other.getExamDate().equals(target.getExamDate())
                    && other.getStartTime().isBefore(target.getEndTime())
                    && other.getEndTime().isAfter(target.getStartTime());
        });
    }

    private int requiredInvigilators(Integer examSessionId, Integer venueId) {
        long allocated = allocationRepository.countByVenueIdAndExamSessionId(venueId, examSessionId);
        return Math.max(1, (int) Math.ceil(allocated / (double) STUDENTS_PER_INVIGILATOR));
    }

    private AdminStaffingResponse staffingFor(Integer examSessionId, Integer venueId, ExamSession exam,
                                              List<Staff> activeInvigilators,
                                              List<InvigilatorAssignment> examAssignments) {
        List<InvigilatorAssignment> assignments = assignmentRepository
                .findByExamSessionIdAndVenueId(examSessionId, venueId);
        long allocated = allocationRepository.countByVenueIdAndExamSessionId(venueId, examSessionId);
        int required = Math.max(1, (int) Math.ceil(allocated / (double) STUDENTS_PER_INVIGILATOR));
        long active = assignments.stream().filter(a -> !"CANCELLED".equals(a.getAssignmentStatus())).count();
        long drafts = assignments.stream().filter(a -> "DRAFT".equals(a.getAssignmentStatus())).count();
        long published = assignments.stream().filter(a -> "PUBLISHED".equals(a.getAssignmentStatus())).count();
        List<AdminStaffMemberResponse> assigned = assignments.stream()
            .filter(a -> !"CANCELLED".equals(a.getAssignmentStatus()))
            .map(a -> staffRepository.findById(a.getStaffId())
                .map(staff -> new AdminStaffMemberResponse(
                    staff.getStaffId(), staff.getFullName(), a.getAssignmentStatus()))
                .orElse(null))
            .filter(java.util.Objects::nonNull)
            .toList();
        List<Integer> assignedStaffIds = examAssignments.stream()
            .filter(a -> !"CANCELLED".equals(a.getAssignmentStatus()))
            .map(a -> a.getStaffId())
            .toList();
        List<AdminStaffMemberResponse> remaining = activeInvigilators.stream()
            .filter(staff -> !assignedStaffIds.contains(staff.getStaffId()))
            .filter(staff -> !hasConflict(staff.getStaffId(), exam, null))
            .map(staff -> new AdminStaffMemberResponse(staff.getStaffId(), staff.getFullName(), null))
            .toList();
        return new AdminStaffingResponse(examSessionId, venueId, allocated, required, active, drafts, published,
            active >= required ? "STAFFED" : "UNDERSTAFFED", assigned, remaining,
            activeInvigilators.size(), assignedStaffIds.stream().distinct().count(), remaining.size());
    }

    private AdminAssignmentResponse toResponse(InvigilatorAssignment assignment, Staff staff) {
        return new AdminAssignmentResponse(assignment.getExamSessionId(), assignment.getVenueId(),
                assignment.getStaffId(), staff.getFullName(), assignment.getAssignmentStatus(),
                assignment.getAssignedAt(), assignment.getPublishedAt(), assignment.getAssignmentNotes());
    }
}