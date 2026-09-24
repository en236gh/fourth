package com.backend.fourth.exam;

import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.dashboard.controller.DashboardController;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.exam.repository.ExamVenueRepository;
import com.backend.fourth.exam.service.ExamService;
import com.backend.fourth.exam.service.LecturerCourseAccess;
import com.backend.fourth.student.repository.StudentRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LecturerExamScopeTest {
    @Mock ExamSessionRepository exams;
    @Mock ExamVenueRepository venues;
    @Mock StudentRegistrationRepository registrations;
    @Mock StudentVenueAllocationRepository allocations;
    @Mock LecturerCourseAccess access;
    @InjectMocks ExamService examService;
    @InjectMocks DashboardController dashboard;

    private ExamSession exam() {
        var exam = new ExamSession();
        exam.setExamSessionId(22);
        exam.setCourseCode("CSC1202");
        exam.setAcademicYear("2026/2027");
        exam.setSemester(1);
        return exam;
    }

    @Test
    void registrationAndVenueReadsCheckOwnershipBeforeLoadingDetails() {
        var exam = exam();
        when(exams.findById(22)).thenReturn(Optional.of(exam));
        doThrow(new AccessDeniedException("Not assigned")).when(access).requireReadAccess(exam);
        assertThrows(AccessDeniedException.class, () -> examService.listRegisteredStudents(22));
        assertThrows(AccessDeniedException.class, () -> examService.listExamVenues(22));
        verifyNoInteractions(registrations, venues);
    }

    @Test
    void dashboardCountsOnlyAssignedExaminations() {
        var exam = exam();
        when(access.myExams()).thenReturn(List.of(exam));
        when(access.myCourseCodes()).thenReturn(List.of("CSC1202"));
        when(registrations.countByCourseCodeAndAcademicYearAndSemester("CSC1202", "2026/2027", 1))
                .thenReturn(30L);
        when(allocations.countByExamSessionId(22)).thenReturn(25L);
        var data = dashboard.lecturerDashboard(null).data();
        assertEquals(1, data.get("totalExaminations"));
        assertEquals(30L, data.get("registeredStudents"));
        assertEquals(25L, data.get("allocatedStudents"));
        assertEquals(List.of("CSC1202"), data.get("courseCodes"));
        verify(registrations, never()).count();
        verify(allocations, never()).count();
        verifyNoInteractions(exams);
    }

    @Test
    void dashboardRejectsAnotherLecturersExam() {
        var exam = exam();
        when(exams.findById(22)).thenReturn(Optional.of(exam));
        doThrow(new AccessDeniedException("Not assigned")).when(access).requireAssigned(exam);
        assertThrows(AccessDeniedException.class, () -> dashboard.lecturerDashboard(22));
        verifyNoInteractions(registrations, allocations);
    }

    @Test
    void examResponseContainsOnlyVisibleExams() {
        when(access.visibleExams()).thenReturn(List.of(exam()));
        var response = examService.listExams();
        assertEquals(1, response.size());
        assertEquals("CSC1202", response.getFirst().courseCode());
        verifyNoInteractions(exams);
    }
}
