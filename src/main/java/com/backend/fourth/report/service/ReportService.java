package com.backend.fourth.report.service;

import com.backend.fourth.attendance.entity.Attendance;
import com.backend.fourth.attendance.entity.AttendanceStatus;
import com.backend.fourth.attendance.repository.AttendanceRepository;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.CourseLecturerRepository;
import com.backend.fourth.incident.entity.Incident;
import com.backend.fourth.incident.repository.IncidentRepository;
import com.backend.fourth.report.entity.GeneratedReport;
import com.backend.fourth.report.repository.GeneratedReportRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.venue.entity.Venue;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
    private static final Font TABLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 7);

    private final AttendanceRepository attendanceRepository;
    private final GeneratedReportRepository generatedReportRepository;
    private final IncidentRepository incidentRepository;
    private final CourseLecturerRepository courseLecturerRepository;

    public ReportService(
            AttendanceRepository attendanceRepository,
            GeneratedReportRepository generatedReportRepository,
            IncidentRepository incidentRepository,
            CourseLecturerRepository courseLecturerRepository) {
        this.attendanceRepository = attendanceRepository;
        this.generatedReportRepository = generatedReportRepository;
        this.incidentRepository = incidentRepository;
        this.courseLecturerRepository = courseLecturerRepository;
    }

    @Transactional
    public GeneratedReport generateExamReport(ExamSession examSession, Staff generatedBy, Venue venue) throws Exception {
        Path output = Files.createTempFile("exam-report-", ".pdf");
        List<Attendance> attendanceList = attendanceRepository.findDetailedByExamSessionId(examSession.getExamSessionId())
                .stream()
                .filter(attendance -> attendance.getVenue() != null
                        && venue.getVenueId().equals(attendance.getVenue().getVenueId()))
                .toList();
        writePdf(output, examSession, attendanceList, venue);

        GeneratedReport report = new GeneratedReport();
        report.setExamSession(examSession);
        report.setGeneratedBy(generatedBy);
        report.setTitle("Attendance Report - " + examSession.getExamSessionId());
        report.setReportType("EXAMINATION_ATTENDANCE");
        report.setFilePath(output.toString());
        report.setGeneratedAt(LocalDateTime.now());
        report.setSummary("Present=" + count(attendanceList, AttendanceStatus.PRESENT));
        return generatedReportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public byte[] generateLecturerReport(ExamSession examSession, Staff lecturer) {
        if (!courseLecturerRepository.existsByCourseCodeAndStaffId(
                examSession.getCourseCode(), lecturer.getStaffId())) {
            throw new AccessDeniedException("You are not assigned to this course");
        }

        List<Attendance> attendance = attendanceRepository
                .findDetailedByExamSessionId(examSession.getExamSessionId());
        List<Incident> incidents = incidentRepository
                .findDetailedByExamSessionId(examSession.getExamSessionId());

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            writeLecturerPdf(output, examSession, lecturer, attendance, incidents);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate the attendance report", exception);
        }
    }

    private void writePdf(Path path, ExamSession examSession, List<Attendance> attendanceList, Venue venue) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        PdfWriter.getInstance(document, new FileOutputStream(path.toFile()));
        document.open();
        addUniversityHeader(document);
        document.add(new Paragraph("Digital Examination Attendance Report"));
        document.add(new Paragraph("Exam Session: " + examSession.getExamSessionId()));
        document.add(new Paragraph("Course: " + examSession.getCourseCode()));
        document.add(new Paragraph("Venue: " + venue.getVenueName()));
        document.add(new Paragraph("Present: " + count(attendanceList, AttendanceStatus.PRESENT)));
        document.add(new Paragraph("Absent: " + count(attendanceList, AttendanceStatus.ABSENT)));
        document.add(new Paragraph("Late: " + count(attendanceList, AttendanceStatus.LATE)));
        document.add(new Paragraph("Wrong Venue: " + count(attendanceList, AttendanceStatus.WRONG_VENUE)));
        addAttendanceSections(document, attendanceList);
        document.close();
    }

    private void writeLecturerPdf(
            OutputStream output,
            ExamSession examSession,
            Staff lecturer,
            List<Attendance> attendance,
            List<Incident> incidents) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        PdfWriter.getInstance(document, output);
        document.open();
        addUniversityHeader(document);

        Paragraph title = new Paragraph("Examination Attendance and Incident Report", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);
        document.add(new Paragraph("Course: " + examSession.getCourseCode()));
        document.add(new Paragraph("Exam date: " + examSession.getExamDate()
                + "  |  Time: " + examSession.getStartTime() + " - " + examSession.getEndTime()));
        document.add(new Paragraph("Academic year: " + examSession.getAcademicYear()
                + "  |  Semester: " + examSession.getSemester()
                + "  |  Type: " + examSession.getExamType()));
        document.add(new Paragraph("Lecturer: " + lecturer.getFullName()));
        document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATE_TIME_FORMAT)));

        addAttendanceSections(document, attendance);

        Paragraph incidentHeading = new Paragraph(
                "Incident report (" + incidents.size() + ")", SECTION_FONT);
        incidentHeading.setSpacingBefore(14);
        incidentHeading.setSpacingAfter(6);
        document.add(incidentHeading);
        if (incidents.isEmpty()) {
            document.add(new Paragraph("No incidents were reported for this examination."));
        } else {
            document.add(buildIncidentTable(incidents));
        }

        document.close();
    }

    private void addUniversityHeader(Document document) throws IOException, DocumentException {
        try (var logoStream = ReportService.class.getResourceAsStream("/images/unza-logo.png")) {
            if (logoStream == null) {
                throw new IOException("UNZA logo resource is missing");
            }
            Image logo = Image.getInstance(logoStream.readAllBytes());
            logo.scaleToFit(72, 72);
            logo.setAlignment(Element.ALIGN_CENTER);
            document.add(logo);
        }
        Paragraph university = new Paragraph("UNIVERSITY OF ZAMBIA", SECTION_FONT);
        university.setAlignment(Element.ALIGN_CENTER);
        university.setSpacingAfter(8);
        document.add(university);
    }

    private void addAttendanceSections(Document document, List<Attendance> attendance) throws DocumentException {
        List<Attendance> attendees = attendance.stream().filter(this::wasInAttendance).toList();
        List<Attendance> absentees = attendance.stream()
                .filter(row -> row.getAttendanceStatus() == AttendanceStatus.ABSENT).toList();
        Paragraph attendanceHeading = new Paragraph(
                "Students in attendance (" + attendees.size() + ")", SECTION_FONT);
        attendanceHeading.setSpacingBefore(14);
        attendanceHeading.setSpacingAfter(6);
        document.add(attendanceHeading);
        document.add(new Paragraph(
                "Includes present, late, and wrong-venue check-ins.",
                TABLE_FONT));

        if (attendees.isEmpty()) {
            document.add(new Paragraph("No students were recorded as attending this examination."));
        } else {
            document.add(buildAttendanceTable(attendees));
        }

        Paragraph absentHeading = new Paragraph("Absent students (" + absentees.size() + ")", SECTION_FONT);
        absentHeading.setSpacingBefore(14);
        absentHeading.setSpacingAfter(6);
        document.add(absentHeading);
        document.add(new Paragraph(
                "Recorded absences only. Absences are finalized when the examination is completed.", TABLE_FONT));
        if (absentees.isEmpty()) {
            document.add(new Paragraph("No students were recorded as absent from this examination."));
        } else {
            PdfPTable table = new PdfPTable(new float[]{0.5f, 1.2f, 2.1f, 1.4f, 1.2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(5);
            addHeaders(table, "#", "Computer No.", "Student", "Programme", "Assigned venue");
            for (int index = 0; index < absentees.size(); index++) {
                Attendance absent = absentees.get(index);
                addCell(table, String.valueOf(index + 1));
                addCell(table, absent.getStudent().getComputerNumber());
                addCell(table, absent.getStudent().getFullName());
                addCell(table, absent.getStudent().getProgram());
                addCell(table, absent.getVenue() == null ? "N/A" : absent.getVenue().getVenueName());
            }
            document.add(table);
        }
    }

    private PdfPTable buildAttendanceTable(List<Attendance> attendees) {
        PdfPTable table = new PdfPTable(new float[]{0.5f, 1.2f, 2.1f, 1.4f, 1.2f, 1.2f, 1.1f, 0.8f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        addHeaders(table, "#", "Computer No.", "Student", "Programme", "Venue", "Check-in", "Status", "Script");

        for (int index = 0; index < attendees.size(); index++) {
            Attendance attendance = attendees.get(index);
            addCell(table, String.valueOf(index + 1));
            addCell(table, attendance.getStudent().getComputerNumber());
            addCell(table, attendance.getStudent().getFullName());
            addCell(table, attendance.getStudent().getProgram());
            addCell(table, attendance.getVenue().getVenueName());
            addCell(table, attendance.getCheckInTime().format(DATE_TIME_FORMAT));
            addCell(table, attendance.getAttendanceStatus().name().replace('_', ' '));
            addCell(table, Boolean.TRUE.equals(attendance.getScriptsSubmitted()) ? "Yes" : "No");
        }
        return table;
    }

    private PdfPTable buildIncidentTable(List<Incident> incidents) {
        PdfPTable table = new PdfPTable(new float[]{1.2f, 1.2f, 0.8f, 1.2f, 1.1f, 1.3f, 3.2f});
        table.setWidthPercentage(100);
        addHeaders(table, "Occurred", "Type", "Severity", "Student", "Venue", "Reported by", "Description");

        for (Incident incident : incidents) {
            addCell(table, incident.getOccurredAt().format(DATE_TIME_FORMAT));
            addCell(table, incident.getIncidentType().name().replace('_', ' '));
            addCell(table, incident.getSeverity());
            addCell(table, incident.getStudent() == null
                    ? "N/A"
                    : incident.getStudent().getComputerNumber() + " - " + incident.getStudent().getFullName());
            addCell(table, incident.getVenue() == null ? "N/A" : incident.getVenue().getVenueName());
            addCell(table, incident.getReportedBy().getFullName());
            addCell(table, incident.getDescription());
        }
        return table;
    }

    private void addHeaders(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, TABLE_HEADER_FONT));
            cell.setBackgroundColor(new BaseColor(45, 76, 115));
            cell.setPadding(5);
            table.addCell(cell);
        }
        table.setHeaderRows(1);
    }

    private void addCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, TABLE_FONT));
        cell.setPadding(4);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private boolean wasInAttendance(Attendance attendance) {
        return attendance.getAttendanceStatus() == AttendanceStatus.PRESENT
                || attendance.getAttendanceStatus() == AttendanceStatus.LATE
                || attendance.getAttendanceStatus() == AttendanceStatus.WRONG_VENUE;
    }

    private long count(List<Attendance> attendanceList, AttendanceStatus status) {
        return attendanceList.stream().filter(attendance -> attendance.getAttendanceStatus() == status).count();
    }
}
