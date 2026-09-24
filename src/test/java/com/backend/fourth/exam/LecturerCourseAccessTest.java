package com.backend.fourth.exam;

import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.exam.entity.CourseLecturer;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.CourseLecturerRepository;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.exam.service.LecturerCourseAccess;
import com.backend.fourth.staff.entity.Staff;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LecturerCourseAccessTest {
    @Mock CurrentStaffResolver resolver;
    @Mock CourseLecturerRepository courses;
    @Mock ExamSessionRepository exams;
    @InjectMocks LecturerCourseAccess access;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private void lecturer() {
        var staff = new Staff();
        staff.setStaffId(12);
        when(resolver.requireCurrentStaff()).thenReturn(staff);
        authenticate("LECTURER");
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("lecturer2@gmail.com", "unused",
                        List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    void assignedLecturerCanAccessOwnCourseOnly() {
        lecturer();
        var own = new ExamSession();
        own.setCourseCode("CSC1202");
        var other = new ExamSession();
        other.setCourseCode("CSC1101");
        when(courses.existsByCourseCodeAndStaffId("CSC1202", 12)).thenReturn(true);
        assertDoesNotThrow(() -> access.requireAssigned(own));
        assertThrows(AccessDeniedException.class, () -> access.requireAssigned(other));
        assertThrows(AccessDeniedException.class, () -> access.requireReadAccess(other));
    }

    @Test
    void lecturerExamListUsesOnlyAssignedCourses() {
        lecturer();
        var link = new CourseLecturer();
        link.setCourseCode("CSC1202");
        link.setStaffId(12);
        var exam = new ExamSession();
        exam.setCourseCode("CSC1202");
        when(courses.findByStaffIdOrderByCourseCodeAsc(12)).thenReturn(List.of(link));
        when(exams.findByCourseCodeIn(List.of("CSC1202"))).thenReturn(List.of(exam));
        assertEquals(List.of(exam), access.visibleExams());
        verify(exams, never()).findAll();
    }

    @Test
    void unassignedLecturerSeesNoExams() {
        lecturer();
        when(courses.findByStaffIdOrderByCourseCodeAsc(12)).thenReturn(List.of());
        assertTrue(access.visibleExams().isEmpty());
        verifyNoInteractions(exams);
    }

    @Test
    void administratorRetainsAllExamReadAccess() {
        authenticate("ADMINISTRATOR");
        var exam = new ExamSession();
        when(exams.findAll()).thenReturn(List.of(exam));
        assertEquals(List.of(exam), access.visibleExams());
        assertDoesNotThrow(() -> access.requireReadAccess(exam));
        verifyNoInteractions(courses, resolver);
    }
}
