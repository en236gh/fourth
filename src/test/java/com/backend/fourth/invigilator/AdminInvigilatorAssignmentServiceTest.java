package com.backend.fourth.invigilator;

import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.entity.ExamVenue;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.exam.repository.ExamVenueRepository;
import com.backend.fourth.invigilator.dto.AutoAssignmentResponse;
import com.backend.fourth.invigilator.entity.InvigilatorAssignment;
import com.backend.fourth.invigilator.repository.InvigilatorAssignmentRepository;
import com.backend.fourth.staff.entity.Role;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.staff.repository.StaffRepository;
import com.backend.fourth.invigilator.service.AdminInvigilatorAssignmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInvigilatorAssignmentServiceTest {

    @Mock
    private InvigilatorAssignmentRepository assignmentRepository;
    @Mock
    private ExamSessionRepository examSessionRepository;
    @Mock
    private ExamVenueRepository examVenueRepository;
    @Mock
    private StudentVenueAllocationRepository allocationRepository;
    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private AdminInvigilatorAssignmentService service;

    @Test
    void shouldCreateDraftsForRequiredActiveNonConflictingInvigilators() {
        ExamSession target = exam(10, LocalTime.of(9, 0), LocalTime.of(11, 0));
        ExamSession clash = exam(20, LocalTime.of(9, 30), LocalTime.of(10, 30));
        ExamVenue venue = venue(10, 1);
        Staff first = staff(1, "First", "ACTIVE", true);
        Staff second = staff(2, "Second", "ACTIVE", true);
        Staff inactive = staff(3, "Inactive", "INACTIVE", true);
        Staff conflicting = staff(4, "Conflicting", "ACTIVE", true);
        InvigilatorAssignment conflictingAssignment = assignment(20, 2, 4, "PUBLISHED");

        when(examSessionRepository.findById(10)).thenReturn(Optional.of(target));
        when(examSessionRepository.findById(20)).thenReturn(Optional.of(clash));
        when(examVenueRepository.findByExamSessionIdOrderByVenueIdAsc(10)).thenReturn(List.of(venue));
        when(allocationRepository.countByVenueIdAndExamSessionId(1, 10)).thenReturn(101L);
        when(staffRepository.findAll()).thenReturn(List.of(first, second, inactive, conflicting));
        when(assignmentRepository.findByExamSessionIdAndVenueId(10, 1)).thenReturn(List.of());
        when(assignmentRepository.findByStaffId(1)).thenReturn(List.of());
        when(assignmentRepository.findByStaffId(2)).thenReturn(List.of());
        when(assignmentRepository.findByStaffId(4)).thenReturn(List.of(conflictingAssignment));
        when(assignmentRepository.save(any(InvigilatorAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AutoAssignmentResponse response = service.autoAssignDrafts(10, first);

        assertEquals(2, response.createdDraftAssignments());
        assertEquals(List.of(1), response.understaffedVenueIds());
        assertEquals(2, response.assignments().stream()
                .filter(assignment -> "DRAFT".equals(assignment.assignmentStatus()))
                .count());
        assertTrue(response.assignments().stream().noneMatch(assignment -> assignment.staffId().equals(3)));
        assertTrue(response.assignments().stream().noneMatch(assignment -> assignment.staffId().equals(4)));
    }

    @Test
    void shouldDeleteDraftWhenCancelled() {
        ExamSession target = exam(10, LocalTime.of(9, 0), LocalTime.of(11, 0));
        InvigilatorAssignment draft = assignment(10, 1, 2, "DRAFT");
        Staff invigilator = staff(2, "Invigilator", "ACTIVE", true);

        when(examSessionRepository.findById(10)).thenReturn(Optional.of(target));
        when(assignmentRepository.findById(any())).thenReturn(Optional.of(draft));
        when(staffRepository.findById(2)).thenReturn(Optional.of(invigilator));

        service.cancel(10, 1, 2);

        verify(assignmentRepository).deleteById(any());
    }

    @Test
    void shouldDeletePublishedWhenCancelled() {
        ExamSession target = exam(10, LocalTime.of(9, 0), LocalTime.of(11, 0));
        InvigilatorAssignment published = assignment(10, 1, 2, "PUBLISHED");
        Staff invigilator = staff(2, "Invigilator", "ACTIVE", true);

        when(examSessionRepository.findById(10)).thenReturn(Optional.of(target));
        when(assignmentRepository.findById(any())).thenReturn(Optional.of(published));
        when(staffRepository.findById(2)).thenReturn(Optional.of(invigilator));

        service.cancel(10, 1, 2);

        verify(assignmentRepository).deleteById(any());
    }

    private ExamSession exam(Integer id, LocalTime start, LocalTime end) {
        ExamSession exam = new ExamSession();
        exam.setExamSessionId(id);
        exam.setExamDate(LocalDate.of(2026, 10, 1));
        exam.setStartTime(start);
        exam.setEndTime(end);
        exam.setStatus("SCHEDULED");
        return exam;
    }

    private ExamVenue venue(Integer examSessionId, Integer venueId) {
        ExamVenue venue = new ExamVenue();
        venue.setExamSessionId(examSessionId);
        venue.setVenueId(venueId);
        return venue;
    }

    private Staff staff(Integer id, String name, String status, boolean invigilator) {
        Staff staff = new Staff();
        staff.setStaffId(id);
        staff.setFullName(name);
        staff.setAccountStatus(status);
        if (invigilator) {
            Role role = new Role();
            role.setName("INVIGILATOR");
            staff.getRoles().add(role);
        }
        return staff;
    }

    private InvigilatorAssignment assignment(Integer examSessionId, Integer venueId,
                                             Integer staffId, String status) {
        InvigilatorAssignment assignment = new InvigilatorAssignment();
        assignment.setExamSessionId(examSessionId);
        assignment.setVenueId(venueId);
        assignment.setStaffId(staffId);
        assignment.setAssignmentStatus(status);
        return assignment;
    }
}