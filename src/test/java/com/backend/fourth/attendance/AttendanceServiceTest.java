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
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Mock private com.backend.fourth.student.repository.StudentRegistrationRepository registrationRepository;
    @Mock private com.backend.fourth.exam.service.LecturerCourseAccess lecturerCourseAccess;
    @Mock private com.backend.fourth.common.security.CurrentStaffResolver currentStaffResolver;
    @Mock private com.backend.fourth.face.repository.StudentFaceTemplateRepository faceTemplateRepository;
    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void shouldRejectDuplicateAttendanceForSameStudentAndExamSession() {
        when(registrationRepository.existsByComputerNumberAndCourseCodeAndAcademicYearAndSemester(any(), any(), any(), any())).thenReturn(true);
        CheckInRequest request = new CheckInRequest("2022004264", 1, 1, "COMPUTER");
        Staff invigilator = createStaff();

        when(assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(1, 1, 2, "PUBLISHED"))
                .thenReturn(true);
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
        when(registrationRepository.existsByComputerNumberAndCourseCodeAndAcademicYearAndSemester(any(), any(), any(), any())).thenReturn(true);
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
        when(assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(5, 16, 2, "PUBLISHED")).thenReturn(true);
        Venue venue = new Venue();
        venue.setVenueId(16);
        venue.setVenueName("Main LT 1");
        when(venueRepository.findById(16)).thenReturn(Optional.of(venue));
        when(attendanceRepository.findByStudentComputerNumberAndExamSessionExamSessionId("2022004264", 5))
                .thenReturn(Optional.empty());

        when(faceTemplateRepository.existsById("2022004264")).thenReturn(true);

        var response = attendanceService.lookupStudentByQr(new QrLookupRequest(token, 5), createStaff());
        assertEquals("2022004264", response.computerNumber());
        assertEquals("Main LT 1", response.allocatedVenueName());
        assertTrue(response.faceEnrolled());
    }

    @Test
    void plainCheckInCannotClaimFaceVerification() {
        for (String method : new String[]{"QR_AND_FACE", "FACE_RECOGNITION", "QR_AND_FACIAL"}) {
            assertThrows(IllegalArgumentException.class, () -> attendanceService.checkIn(
                    new CheckInRequest("2022004264", 1, 1, method), createStaff()));
        }
        org.mockito.Mockito.verifyNoInteractions(attendanceRepository, assignmentRepository);
    }

    @Test
    void faceCheckInStoresScoreAndOverrideReason() {
        when(registrationRepository.existsByComputerNumberAndCourseCodeAndAcademicYearAndSemester(any(), any(), any(), any())).thenReturn(true);
        when(assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(1, 1, 2, "PUBLISHED")).thenReturn(true);
        when(studentRepository.findByComputerNumber("2022004264")).thenReturn(Optional.of(createStudent()));
        when(examSessionRepository.findById(1)).thenReturn(Optional.of(createExamSession()));
        when(venueRepository.findById(1)).thenReturn(Optional.of(createVenue()));
        when(allocationRepository.findByComputerNumberAndExamSessionId("2022004264", 1)).thenReturn(Optional.of(createAllocation()));
        when(attendanceRepository.findByStudentComputerNumberAndExamSessionExamSessionId("2022004264", 1)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = attendanceService.checkInWithFace("2022004264", 1, 1, createStaff(), 0.38f, "Checked NRC", "alert");

        assertEquals(com.backend.fourth.attendance.entity.VerificationMethod.QR_AND_FACE, response.verificationMethod());
        assertEquals(0.38f, response.faceMatchScore());
        assertEquals("alert", response.alertMessage());
        var saved = org.mockito.ArgumentCaptor.forClass(Attendance.class);
        org.mockito.Mockito.verify(attendanceRepository).save(saved.capture());
        assertEquals("Checked NRC", saved.getValue().getFaceOverrideReason());
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
        when(registrationRepository.existsByComputerNumberAndCourseCodeAndAcademicYearAndSemester(any(), any(), any(), any())).thenReturn(true);
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
        when(assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(5, 16, 2, "PUBLISHED")).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> attendanceService.lookupStudentByQr(new QrLookupRequest(token, 5), createStaff()));
    }

    @Test
    void shouldAllowQrCheckInWhenSessionHasNotStartedYet() {
        when(registrationRepository.existsByComputerNumberAndCourseCodeAndAcademicYearAndSemester(any(), any(), any(), any())).thenReturn(true);
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
        when(assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(5, 16, 2, "PUBLISHED"))
                .thenReturn(true);
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


    @Test void draftExamCannotBeLookedUpEvenWithAStudentRecord() {
        ExamSession exam=createExamSession();exam.setSchedulePublished(false);
        when(studentRepository.findByComputerNumber("2022004264")).thenReturn(Optional.of(createStudent()));
        when(examSessionRepository.findById(1)).thenReturn(Optional.of(exam));
        assertThrows(IllegalStateException.class,()->attendanceService.lookupStudent("2022004264",1,createStaff()));
        org.mockito.Mockito.verifyNoInteractions(allocationRepository);
    }

    @Test void staleAllocationCannotAuthorizeUnregisteredStudent() {
        when(studentRepository.findByComputerNumber("2022004264")).thenReturn(Optional.of(createStudent()));
        when(examSessionRepository.findById(1)).thenReturn(Optional.of(createExamSession()));
        assertThrows(IllegalArgumentException.class,()->attendanceService.lookupStudent("2022004264",1,createStaff()));
        org.mockito.Mockito.verifyNoInteractions(allocationRepository);
    }

    @Test void checkInRejectsWrongExamVenueInsteadOfOverridingAllocation() {
        when(assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(1,2,2,"PUBLISHED")).thenReturn(true);
        when(studentRepository.findByComputerNumber("2022004264")).thenReturn(Optional.of(createStudent()));
        when(examSessionRepository.findById(1)).thenReturn(Optional.of(createExamSession()));
        when(registrationRepository.existsByComputerNumberAndCourseCodeAndAcademicYearAndSemester(any(),any(),any(),any())).thenReturn(true);
        when(venueRepository.findById(2)).thenReturn(Optional.of(createVenue()));
        when(allocationRepository.findByComputerNumberAndExamSessionId("2022004264",1)).thenReturn(Optional.of(createAllocation()));
        assertThrows(IllegalArgumentException.class,()->attendanceService.checkIn(new CheckInRequest("2022004264",1,2,"QR_CODE"),createStaff()));
        org.mockito.Mockito.verify(attendanceRepository,org.mockito.Mockito.never()).save(any());
    }

    @Test void attendanceReadChecksLecturerCourseOwnership() {
        ExamSession exam=createExamSession();
        when(examSessionRepository.findById(1)).thenReturn(Optional.of(exam));
        when(lecturerCourseAccess.hasAuthority(any())).thenAnswer(i -> "LECTURER".equals(i.getArgument(0)));
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("Not assigned"))
                .when(lecturerCourseAccess).requireAssigned(exam);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->attendanceService.getAttendanceForExam(1));
        org.mockito.Mockito.verifyNoInteractions(attendanceRepository);
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
