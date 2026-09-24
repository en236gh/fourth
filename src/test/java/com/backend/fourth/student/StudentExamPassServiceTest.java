package com.backend.fourth.student;

import com.backend.fourth.allocation.entity.StudentVenueAllocation;
import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.attendance.entity.Attendance;
import com.backend.fourth.attendance.entity.AttendanceStatus;
import com.backend.fourth.attendance.repository.AttendanceRepository;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.student.dto.StudentExaminationSummaryResponse;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.entity.StudentRegistration;
import com.backend.fourth.student.repository.ExaminationPassRepository;
import com.backend.fourth.student.repository.StudentProgrammeEnrolmentRepository;
import com.backend.fourth.student.repository.StudentRegistrationRepository;
import com.backend.fourth.student.service.ExamPassPdfService;
import com.backend.fourth.student.service.ExamPassQrService;
import com.backend.fourth.student.service.StudentExamPassService;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentExamPassServiceTest {

    @Mock
    private StudentRegistrationRepository studentRegistrationRepository;
    @Mock
    private ExamSessionRepository examSessionRepository;
    @Mock
    private StudentVenueAllocationRepository allocationRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private ExaminationPassRepository examinationPassRepository;
    @Mock
    private StudentProgrammeEnrolmentRepository studentProgrammeEnrolmentRepository;
    @Mock
    private ExamPassQrService examPassQrService;
    @Mock
    private ExamPassPdfService examPassPdfService;
    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private StudentExamPassService studentExamPassService;

    @Test
    void shouldIncludeAttendanceStatusForEachExam() {
        Student student = createStudent();
        StudentRegistration registration = createRegistration();
        ExamSession session = createSession();
        StudentVenueAllocation allocation = createAllocation();
        Venue venue = createVenue();
        Attendance attendance = new Attendance();
        attendance.setAttendanceStatus(AttendanceStatus.PRESENT);

        when(studentRegistrationRepository.findByComputerNumber(student.getComputerNumber()))
                .thenReturn(List.of(registration));
        when(studentProgrammeEnrolmentRepository.findActiveForAcademicYear(student.getComputerNumber(), "2026/2027"))
                .thenReturn(Optional.of(new StudentProgrammeEnrolmentRepository.ProgrammeEnrolment(
                        1, "SNS-BCS", "Bachelor of Computer Science", 1, "ACTIVE")));
        when(examSessionRepository.findByCourseCodeAndAcademicYearAndSemester("CSC1101", "2026/2027", 1))
                .thenReturn(List.of(session));
        when(allocationRepository.findByComputerNumberAndExamSessionId(student.getComputerNumber(), session.getExamSessionId()))
                .thenReturn(Optional.of(allocation));
        when(venueRepository.findById(allocation.getVenueId()))
                .thenReturn(Optional.of(venue));
        when(attendanceRepository.findByStudentComputerNumberAndExamSessionExamSessionId(
                student.getComputerNumber(), session.getExamSessionId()))
                .thenReturn(Optional.of(attendance));

        List<StudentExaminationSummaryResponse> result = studentExamPassService.listMyExaminations(student);

        assertEquals(1, result.size());
        assertEquals("PRESENT", result.get(0).attendanceStatus());
    }

    private Student createStudent() {
        Student student = new Student();
        student.setComputerNumber("2022004264");
        return student;
    }

    private StudentRegistration createRegistration() {
        StudentRegistration registration = new StudentRegistration();
        registration.setComputerNumber("2022004264");
        registration.setCourseCode("CSC1101");
        registration.setAcademicYear("2026/2027");
        registration.setSemester(1);
        return registration;
    }

    private ExamSession createSession() {
        ExamSession session = new ExamSession();
        session.setExamSessionId(101);
        session.setCourseCode("CSC1101");
        session.setAcademicYear("2026/2027");
        session.setSemester(1);
        session.setExamDate(LocalDate.of(2026, 11, 24));
        session.setStartTime(LocalTime.of(9, 0));
        session.setEndTime(LocalTime.of(11, 0));
        session.setExamType("FINAL");
        session.setStatus("SCHEDULED");
        return session;
    }

    private StudentVenueAllocation createAllocation() {
        StudentVenueAllocation allocation = new StudentVenueAllocation();
        allocation.setComputerNumber("2022004264");
        allocation.setExamSessionId(101);
        allocation.setVenueId(42);
        return allocation;
    }

    private Venue createVenue() {
        Venue venue = new Venue();
        venue.setVenueId(42);
        venue.setVenueName("Main LT 1");
        venue.setBuilding("School of Science");
        return venue;
    }
}
