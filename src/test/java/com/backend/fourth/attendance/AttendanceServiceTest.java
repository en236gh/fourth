package com.backend.fourth.attendance;

import com.backend.fourth.allocation.entity.StudentVenueAllocation;
import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.attendance.dto.CheckInRequest;
import com.backend.fourth.attendance.entity.Attendance;
import com.backend.fourth.attendance.repository.AttendanceRepository;
import com.backend.fourth.attendance.service.AttendanceService;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.invigilator.repository.InvigilatorAssignmentRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.staff.repository.StaffRepository;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.repository.StudentRepository;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ExamSessionRepository examSessionRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private StudentVenueAllocationRepository allocationRepository;
    @Mock
    private InvigilatorAssignmentRepository assignmentRepository;
    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void shouldRejectDuplicateAttendanceForSameStudentAndExamSession() {
        CheckInRequest request = new CheckInRequest("2022004264", 1, 1, "COMPUTER");
        Staff invigilator = createStaff();

        when(assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffId(1, 1, 2)).thenReturn(true);
        when(studentRepository.findByComputerNumber("2022004264")).thenReturn(Optional.of(createStudent()));
        when(examSessionRepository.findById(1)).thenReturn(Optional.of(createExamSession()));
        when(venueRepository.findById(1)).thenReturn(Optional.of(createVenue()));
        when(allocationRepository.findByComputerNumberAndExamSessionId("2022004264", 1))
                .thenReturn(Optional.of(createAllocation()));
        when(attendanceRepository.findByStudentComputerNumberAndExamSessionExamSessionId("2022004264", 1))
                .thenReturn(Optional.of(new Attendance()));

        assertThrows(IllegalStateException.class, () -> attendanceService.checkIn(request, invigilator));
    }

    private Staff createStaff() {
        Staff staff = new Staff();
        staff.setStaffId(2);
        return staff;
    }

    private Student createStudent() {
        Student student = new Student();
        student.setComputerNumber("2022004264");
        return student;
    }

    private ExamSession createExamSession() {
        ExamSession examSession = new ExamSession();
        examSession.setExamSessionId(1);
        examSession.setStatus("IN_PROGRESS");
        return examSession;
    }

    private Venue createVenue() {
        Venue venue = new Venue();
        venue.setVenueId(1);
        return venue;
    }

    private StudentVenueAllocation createAllocation() {
        StudentVenueAllocation allocation = new StudentVenueAllocation();
        allocation.setComputerNumber("2022004264");
        allocation.setExamSessionId(1);
        allocation.setVenueId(1);
        return allocation;
    }
}
