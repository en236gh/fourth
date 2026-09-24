package com.backend.fourth.invigilator;

import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.entity.ExamVenue;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.exam.repository.ExamVenueRepository;
import com.backend.fourth.invigilator.dto.AutoAssignmentResponse;
import com.backend.fourth.invigilator.dto.AcademicSelection;
import com.backend.fourth.invigilator.dto.CreateInvigilatorAssignmentRequest;
import com.backend.fourth.invigilator.repository.AssignmentAcademicRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

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
    @Mock
    private AssignmentAcademicRepository academicRepository;
    @Mock
    private com.backend.fourth.invigilator.repository.AssignmentWriteLock assignmentWriteLock;

    private final AcademicSelection selection = new AcademicSelection(1, 2, 3, "CSC3101");

    @InjectMocks
    private AdminInvigilatorAssignmentService service;

    @Test
    void bulkPublishDeduplicatesExamsAndPublishesDrafts() {
        when(examSessionRepository.findById(10)).thenReturn(Optional.of(exam(10, LocalTime.of(9, 0), LocalTime.of(11, 0))));
        var draft = assignment(10, 1, 2, "DRAFT");
        var published = assignment(10, 2, 2, "PUBLISHED");
        when(assignmentRepository.findByExamSessionId(10)).thenReturn(List.of(draft, published));
        when(staffRepository.findById(2)).thenReturn(Optional.of(staff(2, "Staff", "ACTIVE", true)));
        when(assignmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.publishBulk(List.of(10, 10));
        assertEquals(1, result.size());
        assertEquals("PUBLISHED", result.get(0).assignmentStatus());
        verify(assignmentRepository).findByExamSessionId(10);
    }

    @Test
    void bulkAssignmentAvoidsClashesAndIsSafeToRepeatWithoutHierarchy() {
        ExamSession first = exam(10, LocalTime.of(9, 0), LocalTime.of(11, 0));
        ExamSession second = exam(20, LocalTime.of(10, 0), LocalTime.of(12, 0));
        first.setExamDate(LocalDate.now().plusDays(1));
        second.setExamDate(first.getExamDate());
        Staff invigilator = staff(1, "First", "ACTIVE", true);
        var saved = new java.util.ArrayList<InvigilatorAssignment>();
        when(examSessionRepository.findByStatus("SCHEDULED")).thenReturn(List.of(second, first));
        when(examSessionRepository.findById(10)).thenReturn(Optional.of(first));
        when(examVenueRepository.findByExamSessionIdOrderByVenueIdAsc(10)).thenReturn(List.of(venue(10, 1)));
        when(examVenueRepository.findByExamSessionIdOrderByVenueIdAsc(20)).thenReturn(List.of(venue(20, 2)));
        when(staffRepository.findAll()).thenReturn(List.of(invigilator));
        when(assignmentRepository.findByStaffId(1)).thenAnswer(i -> List.copyOf(saved));
        when(assignmentRepository.findByExamSessionIdAndVenueId(any(), any())).thenAnswer(i ->
                saved.stream().filter(a -> a.getExamSessionId().equals(i.getArgument(0))
                        && a.getVenueId().equals(i.getArgument(1))).toList());
        when(assignmentRepository.save(any())).thenAnswer(i -> {
            InvigilatorAssignment assignment = i.getArgument(0);
            saved.add(assignment);
            return assignment;
        });

        var result = service.autoAssignAllDrafts(invigilator);
        assertEquals(2, result.totalExams());
        assertEquals(1, result.createdDraftAssignments());
        assertEquals(10, result.exams().get(0).examSessionId());
        assertEquals(List.of(2), result.exams().get(1).understaffedVenueIds());
        assertEquals(0, service.autoAssignAllDrafts(invigilator).createdDraftAssignments());
        assertEquals("DRAFT", saved.get(0).getAssignmentStatus());
        verifyNoInteractions(academicRepository);
    }

    @Test
    void bulkAssignmentSkipsPastExamsAndReportsMissingVenues() {
        ExamSession past = exam(10, LocalTime.of(9, 0), LocalTime.of(11, 0));
        past.setExamDate(LocalDate.now().minusDays(1));
        ExamSession future = exam(20, LocalTime.of(9, 0), LocalTime.of(11, 0));
        future.setExamDate(LocalDate.now().plusDays(1));
        when(examSessionRepository.findByStatus("SCHEDULED")).thenReturn(List.of(past, future));
        var result = service.autoAssignAllDrafts(new Staff());
        assertEquals(1, result.totalExams());
        assertEquals(List.of(20), result.examsWithoutVenues());
        assertEquals(0, result.createdDraftAssignments());
        verifyNoInteractions(academicRepository, staffRepository);
        verify(assignmentWriteLock).acquire();
    }

    @Test
    void bulkAssignmentReportsShortagesWhenNoInvigilatorsAreAvailable() {
        ExamSession future = exam(10, LocalTime.of(9, 0), LocalTime.of(11, 0));
        future.setExamDate(LocalDate.now().plusDays(1));
        when(examSessionRepository.findByStatus("SCHEDULED")).thenReturn(List.of(future));
        when(examVenueRepository.findByExamSessionIdOrderByVenueIdAsc(10)).thenReturn(List.of(venue(10, 1)));
        var result = service.autoAssignAllDrafts(new Staff());
        assertEquals(List.of(1), result.exams().get(0).understaffedVenueIds());
        assertEquals(0, result.createdDraftAssignments());
    }

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

        when(academicRepository.matches(10, selection)).thenReturn(true);
        AutoAssignmentResponse response = service.autoAssignDrafts(10, selection, first);

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

    @Test
    void rejectsManualAssignmentOutsideSelectedHierarchy() {
        var request = new CreateInvigilatorAssignmentRequest(
                selection, 10, 1, 2, null);
        assertThrows(IllegalArgumentException.class,
                () -> service.createDraft(request, new Staff()));
        verifyNoInteractions(assignmentRepository, examVenueRepository, staffRepository);
    }

    @Test
    void rejectsAutomaticAssignmentOutsideSelectedHierarchy() {
        assertThrows(IllegalArgumentException.class,
                () -> service.autoAssignDrafts(10, selection, new Staff()));
        verifyNoInteractions(assignmentRepository, examVenueRepository, staffRepository);
    }

    @Test
    void createsManualAssignmentForMatchingHierarchy() {
        when(academicRepository.matches(10, selection)).thenReturn(true);
        when(examSessionRepository.findById(10)).thenReturn(Optional.of(exam(10, LocalTime.of(9, 0), LocalTime.of(11, 0))));
        when(examVenueRepository.existsById(any())).thenReturn(true);
        when(staffRepository.findById(2)).thenReturn(Optional.of(staff(2, "Invigilator", "ACTIVE", true)));
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new CreateInvigilatorAssignmentRequest(
                selection, 10, 1, 2, null);
        var result = service.createDraft(request, staff(1, "Admin", "ACTIVE", false));
        assertEquals(10, result.examSessionId());
        assertEquals("DRAFT", result.assignmentStatus());
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
