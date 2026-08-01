package com.backend.fourth.attendance;

import com.backend.fourth.allocation.entity.StudentVenueAllocation;
import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.attendance.dto.CheckInRequest;
import com.backend.fourth.attendance.dto.QrCheckInRequest;
import com.backend.fourth.attendance.dto.QrLookupRequest;
import com.backend.fourth.attendance.entity.Attendance;
import com.backend.fourth.attendance.repository.AttendanceRepository;
import com.backend.fourth.attendance.service.AttendanceService;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.invigilator.repository.InvigilatorAssignmentRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.staff.repository.StaffRepository;
import com.backend.fourth.student.entity.ExaminationPass;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.repository.ExaminationPassRepository;
import com.backend.fourth.student.repository.StudentRepository;
import com.backend.fourth.student.service.ExamPassQrService;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    @Mock
    private ExaminationPassRepository examinationPassRepository;
    @Mock
    private ExamPassQrService examPassQrService;

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

    @Test
    void shouldLookupStudentFromValidQrToken() {
        String token = "signed.qr.token";
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("2022004264");
        when(claims.getId()).thenReturn("jti-1");
        when(claims.get("academicYear")).thenReturn("2025/2026");
        when(claims.get("semester", Integer.class)).thenReturn(1);
        when(examPassQrService.parseAndValidate(token)).thenReturn(claims);

        ExaminationPass pass = new ExaminationPass();
        pass.setStudent(createStudent());
        pass.setAcademicYear("2025/2026");
        pass.setSemester(1);
        pass.setQrToken(token);
        pass.setQrJti("jti-1");
        when(examinationPassRepository.findByQrJti("jti-1")).thenReturn(Optional.of(pass));
        when(examSessionRepository.findById(5)).thenReturn(Optional.of(createExamSessionForPeriod()));
        when(studentRepository.findByComputerNumber("2022004264")).thenReturn(Optional.of(createStudent()));
        when(allocationRepository.findByComputerNumberAndExamSessionId("2022004264", 5))
                .thenReturn(Optional.of(createAllocationForExam(5)));
        when(assignmentRepository.findByStaffIdAndExamSessionId(2, 5))
                .thenReturn(java.util.List.of(mock(com.backend.fourth.invigilator.entity.InvigilatorAssignment.class)));
        Venue venue = new Venue();
        venue.setVenueId(16);
        venue.setVenueName("Main LT 1");
        when(venueRepository.findById(16)).thenReturn(Optional.of(venue));
        when(attendanceRepository.findByStudentComputerNumberAndExamSessionExamSessionId("2022004264", 5))
                .thenReturn(Optional.empty());

        var response = attendanceService.lookupStudentByQr(new QrLookupRequest(token, 5), createStaff());
        assertEquals("2022004264", response.computerNumber());
        assertEquals("Main LT 1", response.allocatedVenueName());
    }

    @Test
    void shouldRejectRevokedQrToken() {
        String token = "old.qr.token";
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("2022004264");
        when(claims.getId()).thenReturn("jti-old");
        when(claims.get("academicYear")).thenReturn("2025/2026");
        when(claims.get("semester", Integer.class)).thenReturn(1);
        when(examPassQrService.parseAndValidate(token)).thenReturn(claims);
        when(examinationPassRepository.findByQrJti("jti-old")).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> attendanceService.lookupStudentByQr(new QrLookupRequest(token, 5), createStaff()));
    }

    @Test
    void shouldRejectQrLookupWhenInvigilatorNotAssigned() {
        String token = "signed.qr.token";
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("2022004264");
        when(claims.getId()).thenReturn("jti-1");
        when(claims.get("academicYear")).thenReturn("2025/2026");
        when(claims.get("semester", Integer.class)).thenReturn(1);
        when(examPassQrService.parseAndValidate(token)).thenReturn(claims);

        ExaminationPass pass = new ExaminationPass();
        pass.setStudent(createStudent());
        pass.setAcademicYear("2025/2026");
        pass.setSemester(1);
        pass.setQrToken(token);
        pass.setQrJti("jti-1");
        when(examinationPassRepository.findByQrJti("jti-1")).thenReturn(Optional.of(pass));
        when(examSessionRepository.findById(5)).thenReturn(Optional.of(createExamSessionForPeriod()));
        when(studentRepository.findByComputerNumber("2022004264")).thenReturn(Optional.of(createStudent()));
        when(allocationRepository.findByComputerNumberAndExamSessionId("2022004264", 5))
                .thenReturn(Optional.of(createAllocationForExam(5)));
        when(assignmentRepository.findByStaffIdAndExamSessionId(2, 5)).thenReturn(java.util.List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> attendanceService.lookupStudentByQr(new QrLookupRequest(token, 5), createStaff()));
    }

    @Test
    void shouldAllowQrCheckInWhenSessionHasNotStartedYet() {
        String token = "signed.qr.token";
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("2022004264");
        when(claims.getId()).thenReturn("jti-1");
        when(claims.get("academicYear")).thenReturn("2025/2026");
        when(claims.get("semester", Integer.class)).thenReturn(1);
        when(examPassQrService.parseAndValidate(token)).thenReturn(claims);

        ExaminationPass pass = new ExaminationPass();
        pass.setStudent(createStudent());
        pass.setAcademicYear("2025/2026");
        pass.setSemester(1);
        pass.setQrToken(token);
        pass.setQrJti("jti-1");
        when(examinationPassRepository.findByQrJti("jti-1")).thenReturn(Optional.of(pass));

        ExamSession scheduled = createExamSessionForPeriod();
        scheduled.setStatus("SCHEDULED");
        when(examSessionRepository.findById(5)).thenReturn(Optional.of(scheduled));
        when(assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffId(5, 16, 2)).thenReturn(true);
        when(studentRepository.findByComputerNumber("2022004264")).thenReturn(Optional.of(createStudent()));
        when(venueRepository.findById(16)).thenReturn(Optional.of(createVenue()));
        when(allocationRepository.findByComputerNumberAndExamSessionId("2022004264", 5))
                .thenReturn(Optional.of(createAllocationForExam(5)));
        when(attendanceRepository.findByStudentComputerNumberAndExamSessionExamSessionId("2022004264", 5))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(
                () -> attendanceService.checkInByQr(new QrCheckInRequest(token, 5, 16), createStaff()));
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

    private ExamSession createExamSessionForPeriod() {
        ExamSession examSession = new ExamSession();
        examSession.setExamSessionId(5);
        examSession.setAcademicYear("2025/2026");
        examSession.setSemester(1);
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

    private StudentVenueAllocation createAllocationForExam(Integer examSessionId) {
        StudentVenueAllocation allocation = new StudentVenueAllocation();
        allocation.setComputerNumber("2022004264");
        allocation.setExamSessionId(examSessionId);
        allocation.setVenueId(16);
        return allocation;
    }
}
