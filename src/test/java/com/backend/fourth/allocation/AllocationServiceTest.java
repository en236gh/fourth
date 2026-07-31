package com.backend.fourth.allocation;

import com.backend.fourth.allocation.entity.StudentVenueAllocation;
import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.allocation.service.AllocationService;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.entity.ExamVenue;
import com.backend.fourth.exam.repository.ExamVenueRepository;
import com.backend.fourth.student.entity.StudentRegistration;
import com.backend.fourth.student.repository.StudentRegistrationRepository;
import com.backend.fourth.student.repository.StudentRepository;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @InjectMocks
    private AllocationService allocationService;

    @Test
    void shouldAllocateByCapacityFillOrder() {
        ExamSession exam = new ExamSession();
        exam.setExamSessionId(10);
        exam.setCourseCode("CS101");
        exam.setAcademicYear("2025/2026");
        exam.setSemester(1);

        List<StudentRegistration> registrations = IntStream.rangeClosed(1, 310)
                .mapToObj(i -> {
                    StudentRegistration registration = new StudentRegistration();
                    registration.setComputerNumber(String.format("2022%06d", i));
                    return registration;
                })
                .toList();

        ExamVenue ev1 = new ExamVenue();
        ev1.setExamSessionId(10);
        ev1.setVenueId(1);
        ExamVenue ev2 = new ExamVenue();
        ev2.setExamSessionId(10);
        ev2.setVenueId(2);

        Venue venueA = new Venue();
        venueA.setVenueId(1);
        venueA.setCapacity(200);
        Venue venueB = new Venue();
        venueB.setVenueId(2);
        venueB.setCapacity(150);

        when(registrationRepository.findByCourseCodeAndAcademicYearAndSemesterOrderByComputerNumberAsc(
                "CS101", "2025/2026", 1)).thenReturn(registrations);
        when(examVenueRepository.findByExamSessionIdOrderByVenueIdAsc(10)).thenReturn(List.of(ev1, ev2));
        when(venueRepository.findById(1)).thenReturn(Optional.of(venueA));
        when(venueRepository.findById(2)).thenReturn(Optional.of(venueB));
        when(allocationRepository.save(any(StudentVenueAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<StudentVenueAllocation> allocations = allocationService.allocateStudentsToVenues(exam);

        assertEquals(310, allocations.size());
        assertEquals(200, allocations.stream().filter(a -> a.getVenueId().equals(1)).count());
        assertEquals(110, allocations.stream().filter(a -> a.getVenueId().equals(2)).count());
        verify(allocationRepository).deleteByExamSessionId(10);
        verify(allocationRepository, times(310)).save(any(StudentVenueAllocation.class));
    }

    @Test
    void shouldFailWhenStudentsExceedCapacity() {
        ExamSession exam = new ExamSession();
        exam.setExamSessionId(11);
        exam.setCourseCode("CS101");
        exam.setAcademicYear("2025/2026");
        exam.setSemester(1);

        List<StudentRegistration> registrations = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            StudentRegistration registration = new StudentRegistration();
            registration.setComputerNumber("202200426" + i);
            registrations.add(registration);
        }

        ExamVenue ev1 = new ExamVenue();
        ev1.setExamSessionId(11);
        ev1.setVenueId(1);

        Venue venueA = new Venue();
        venueA.setVenueId(1);
        venueA.setCapacity(3);

        when(registrationRepository.findByCourseCodeAndAcademicYearAndSemesterOrderByComputerNumberAsc(
                "CS101", "2025/2026", 1)).thenReturn(registrations);
        when(examVenueRepository.findByExamSessionIdOrderByVenueIdAsc(11)).thenReturn(List.of(ev1));
        when(venueRepository.findById(1)).thenReturn(Optional.of(venueA));

        assertThrows(IllegalStateException.class, () -> allocationService.allocateStudentsToVenues(exam));
    }
}
