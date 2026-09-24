package com.backend.fourth.allocation;

import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.allocation.service.AllocationService;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamVenueRepository;
import com.backend.fourth.exam.service.LecturerCourseAccess;
import org.springframework.security.access.AccessDeniedException;
import com.backend.fourth.student.repository.StudentRegistrationRepository;
import com.backend.fourth.student.repository.StudentRepository;
import com.backend.fourth.venue.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AllocationServiceTest {

    @Mock
    private StudentRegistrationRepository registrationRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private ExamVenueRepository examVenueRepository;
    @Mock
    private StudentVenueAllocationRepository allocationRepository;
    @Mock
    private LecturerCourseAccess lecturerCourseAccess;

    @InjectMocks
    private AllocationService allocationService;

    @Test
    void shouldRejectReadingAnotherLecturersAllocations() {
        ExamSession exam = new ExamSession();
        org.mockito.Mockito.doThrow(new AccessDeniedException("Not assigned"))
                .when(lecturerCourseAccess).requireReadAccess(exam);
        assertThrows(AccessDeniedException.class, () -> allocationService.getAllocationStats(exam));
        org.mockito.Mockito.verifyNoInteractions(registrationRepository, allocationRepository, examVenueRepository);
    }

}
