package com.backend.fourth.report.service;

import com.backend.fourth.attendance.entity.Attendance;
import com.backend.fourth.attendance.entity.AttendanceStatus;
import com.backend.fourth.attendance.repository.AttendanceRepository;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.report.entity.GeneratedReport;
import com.backend.fourth.report.repository.GeneratedReportRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.venue.entity.Venue;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {
    private final AttendanceRepository attendanceRepository;
    private final GeneratedReportRepository generatedReportRepository;

    public ReportService(AttendanceRepository attendanceRepository, GeneratedReportRepository generatedReportRepository) {
        this.attendanceRepository = attendanceRepository;
        this.generatedReportRepository = generatedReportRepository;
    }

    @Transactional
    public GeneratedReport generateExamReport(ExamSession examSession, Staff generatedBy, Venue venue) throws Exception {
        Path output = Files.createTempFile("exam-report-", ".pdf");
        List<Attendance> attendanceList = attendanceRepository.findByExamSessionExamSessionId(examSession.getExamSessionId());
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

    private void writePdf(Path path, ExamSession examSession, List<Attendance> attendanceList, Venue venue) throws FileNotFoundException, DocumentException {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(path.toFile()));
        document.open();
        document.add(new Paragraph("Digital Examination Attendance Report"));
        document.add(new Paragraph("Exam Session: " + examSession.getExamSessionId()));
        document.add(new Paragraph("Course: " + examSession.getCourseCode()));
        document.add(new Paragraph("Venue: " + venue.getVenueName()));
        document.add(new Paragraph("Present: " + count(attendanceList, AttendanceStatus.PRESENT)));
        document.add(new Paragraph("Absent: " + count(attendanceList, AttendanceStatus.ABSENT)));
        document.add(new Paragraph("Late: " + count(attendanceList, AttendanceStatus.LATE)));
        document.add(new Paragraph("Wrong Venue: " + count(attendanceList, AttendanceStatus.WRONG_VENUE)));
        document.close();
    }

    private long count(List<Attendance> attendanceList, AttendanceStatus status) {
        return attendanceList.stream().filter(attendance -> attendance.getAttendanceStatus() == status).count();
    }
}
