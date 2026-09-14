package com.backend.fourth.report;

import com.backend.fourth.attendance.entity.Attendance;
import com.backend.fourth.attendance.entity.AttendanceStatus;
import com.backend.fourth.attendance.repository.AttendanceRepository;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.CourseLecturerRepository;
import com.backend.fourth.incident.entity.Incident;
import com.backend.fourth.incident.entity.IncidentType;
import com.backend.fourth.incident.repository.IncidentRepository;
import com.backend.fourth.report.repository.GeneratedReportRepository;
import com.backend.fourth.report.service.ReportService;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.venue.entity.Venue;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private GeneratedReportRepository generatedReportRepository;
    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private CourseLecturerRepository courseLecturerRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void lecturerReportContainsAttendeesAndIncidentsButExcludesAbsentStudents() throws Exception {
        ExamSession exam = examSession();
        Staff lecturer = staff(8, "Dr Lecturer");
        Attendance present = attendance("2024001", "Present Student", AttendanceStatus.PRESENT);
        Attendance absent = attendance("2024002", "Absent Student", AttendanceStatus.ABSENT);
        Incident incident = incident(exam);

        when(courseLecturerRepository.existsByCourseCodeAndStaffId("CS401", 8)).thenReturn(true);
        when(attendanceRepository.findDetailedByExamSessionId(4)).thenReturn(List.of(present, absent));
        when(incidentRepository.findDetailedByExamSessionId(4)).thenReturn(List.of(incident));

        byte[] pdf = reportService.generateLecturerReport(exam, lecturer);
        String text = pdfText(pdf);

        assertTrue(pdf.length > 0);
        assertTrue(text.contains("Present Student"));
        assertFalse(text.contains("Absent Student"));
        assertTrue(text.contains("PHONE FOUND"));
        assertTrue(text.contains("Phone found under desk"));
    }

    @Test
    void lecturerCannotDownloadReportForAnotherCourse() {
        ExamSession exam = examSession();
        Staff lecturer = staff(8, "Dr Lecturer");
        when(courseLecturerRepository.existsByCourseCodeAndStaffId("CS401", 8)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> reportService.generateLecturerReport(exam, lecturer));
        verifyNoInteractions(attendanceRepository, incidentRepository);
    }

    private ExamSession examSession() {
        ExamSession exam = new ExamSession();
        exam.setExamSessionId(4);
        exam.setCourseCode("CS401");
        exam.setExamDate(LocalDate.of(2026, 8, 23));
        exam.setStartTime(LocalTime.of(9, 0));
        exam.setEndTime(LocalTime.of(12, 0));
        exam.setAcademicYear("2025/2026");
        exam.setSemester(2);
        exam.setExamType("FINAL");
        return exam;
    }

    private Attendance attendance(String computerNumber, String name, AttendanceStatus status) {
        Student student = new Student();
        student.setComputerNumber(computerNumber);
        student.setFullName(name);
        student.setProgram("Computer Science");
        Venue venue = new Venue();
        venue.setVenueName("Main Hall");

        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setVenue(venue);
        attendance.setCheckInTime(LocalDateTime.of(2026, 8, 23, 8, 45));
        attendance.setAttendanceStatus(status);
        attendance.setScriptsSubmitted(true);
        return attendance;
    }

    private Incident incident(ExamSession exam) {
        Incident incident = new Incident();
        incident.setExamSession(exam);
        incident.setIncidentType(IncidentType.PHONE_FOUND);
        incident.setSeverity("MAJOR");
        incident.setDescription("Phone found under desk");
        incident.setOccurredAt(LocalDateTime.of(2026, 8, 23, 10, 15));
        incident.setReportedBy(staff(3, "Invigilator One"));
        return incident;
    }

    private Staff staff(Integer id, String name) {
        Staff staff = new Staff();
        staff.setStaffId(id);
        staff.setFullName(name);
        return staff;
    }

    private String pdfText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(reader, page));
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }
}
