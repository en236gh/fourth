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

    @Mock private com.backend.fourth.attendance.repository.AttendanceRepository attendanceRepository;

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

    @Test
    void oneRegisteredStudentDoesNotCountSixStaleAllocationsOrOtherExamsAtSameVenue() {
        var exam=new ExamSession(); exam.setExamSessionId(1); exam.setCourseCode("CSC1101");
        exam.setAcademicYear("2090/2091"); exam.setSemester(1);
        var registration=new com.backend.fourth.student.entity.StudentRegistration(); registration.setComputerNumber("1");
        org.mockito.Mockito.when(registrationRepository.findByCourseCodeAndAcademicYearAndSemesterOrderByComputerNumberAsc("CSC1101","2090/2091",1)).thenReturn(java.util.List.of(registration));
        var booking=new com.backend.fourth.exam.entity.ExamVenue(); booking.setExamSessionId(1); booking.setVenueId(10);
        org.mockito.Mockito.when(examVenueRepository.findByExamSessionIdOrderByVenueIdAsc(1)).thenReturn(java.util.List.of(booking));
        var venue=new com.backend.fourth.venue.entity.Venue(); venue.setVenueId(10); venue.setCapacity(100); venue.setExaminationCapacity(50);
        org.mockito.Mockito.when(venueRepository.findById(10)).thenReturn(java.util.Optional.of(venue));
        var rows=new java.util.ArrayList<com.backend.fourth.allocation.entity.StudentVenueAllocation>();
        for(int i=1;i<=7;i++) {
            var row=new com.backend.fourth.allocation.entity.StudentVenueAllocation(); row.setComputerNumber(""+i);row.setExamSessionId(1);row.setVenueId(10);rows.add(row);
        }
        org.mockito.Mockito.when(allocationRepository.findByExamSessionId(1)).thenReturn(rows);
        var result=allocationService.getAllocationStats(exam);
        org.junit.jupiter.api.Assertions.assertEquals(1,result.registeredStudents());
        org.junit.jupiter.api.Assertions.assertEquals(1,result.allocatedStudents());
        org.junit.jupiter.api.Assertions.assertEquals(0,result.unallocatedStudents());
        org.junit.jupiter.api.Assertions.assertEquals(6,result.invalidAllocationRecords());
        org.junit.jupiter.api.Assertions.assertEquals(50,result.totalVenueCapacity());
        org.junit.jupiter.api.Assertions.assertEquals(1,result.venueFills().getFirst().allocated());
        org.mockito.Mockito.verify(allocationRepository).findByExamSessionId(1);
        org.mockito.Mockito.verifyNoMoreInteractions(allocationRepository);
    }

}
