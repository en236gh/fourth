package com.backend.fourth.exam.service;

import com.backend.fourth.attendance.service.AttendanceService;
import com.backend.fourth.exam.dto.ExamSessionResponse;
import com.backend.fourth.exam.dto.ExamVenueResponse;
import com.backend.fourth.exam.dto.RegisteredStudentResponse;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.entity.ExamVenue;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.exam.repository.ExamVenueRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.entity.StudentRegistration;
import com.backend.fourth.student.repository.StudentRegistrationRepository;
import com.backend.fourth.student.repository.StudentRepository;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamService {
    private final ExamSessionRepository examSessionRepository;
    private final ExamVenueRepository examVenueRepository;
    private final StudentRegistrationRepository registrationRepository;
    private final StudentRepository studentRepository;
    private final VenueRepository venueRepository;
    private final AttendanceService attendanceService;

    @Transactional(readOnly = true)
    public List<ExamSessionResponse> listExams() {
        return examSessionRepository.findAll().stream()
                .map(this::toExamResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExamSession requireExam(Integer examSessionId) {
        return examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
    }

    @Transactional(readOnly = true)
    public List<RegisteredStudentResponse> listRegisteredStudents(Integer examSessionId) {
        ExamSession exam = requireExam(examSessionId);
        List<StudentRegistration> registrations = registrationRepository
                .findByCourseCodeAndAcademicYearAndSemesterOrderByComputerNumberAsc(
                        exam.getCourseCode(), exam.getAcademicYear(), exam.getSemester());

        List<RegisteredStudentResponse> students = new ArrayList<>();
        for (StudentRegistration registration : registrations) {
            Student student = studentRepository.findByComputerNumber(registration.getComputerNumber())
                    .orElse(null);
            if (student != null) {
                students.add(new RegisteredStudentResponse(
                        student.getComputerNumber(),
                        student.getFullName(),
                        student.getProgram(),
                        student.getYearOfStudy(),
                        student.getPhotoPath(),
                        student.getStatus()));
            }
        }
        return students;
    }

    @Transactional(readOnly = true)
    public List<ExamVenueResponse> listExamVenues(Integer examSessionId) {
        requireExam(examSessionId);
        List<ExamVenueResponse> venues = new ArrayList<>();
        for (ExamVenue examVenue : examVenueRepository.findByExamSessionIdOrderByVenueIdAsc(examSessionId)) {
            Venue venue = venueRepository.findById(examVenue.getVenueId())
                    .orElseThrow(() -> new IllegalArgumentException("Venue not found: " + examVenue.getVenueId()));
            venues.add(new ExamVenueResponse(
                    venue.getVenueId(),
                    venue.getVenueName(),
                    venue.getBuilding(),
                    venue.getCapacity()));
        }
        return venues;
    }

    /**
     * Marks the exam COMPLETED and records ABSENT for any allocated student without attendance.
     * Idempotent if already COMPLETED.
     */
    @Transactional
    public ExamSession completeSession(ExamSession exam, Staff verifiedBy) {
        if ("COMPLETED".equals(exam.getStatus())) {
            return exam;
        }
        exam.setStatus("COMPLETED");
        examSessionRepository.save(exam);
        attendanceService.markAbsenteesForExam(exam, verifiedBy);
        return exam;
    }

    /**
     * Auto-completes IN_PROGRESS exams whose exam_date + end_time has passed.
     */
    @Transactional
    public int completeExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        int completed = 0;
        for (ExamSession exam : examSessionRepository.findByStatus("IN_PROGRESS")) {
            LocalDateTime endsAt = exam.getExamDate().atTime(exam.getEndTime());
            if (!now.isBefore(endsAt)) {
                completeSession(exam, null);
                completed++;
            }
        }
        return completed;
    }

    public ExamSessionResponse toExamResponse(ExamSession exam) {
        return new ExamSessionResponse(
                exam.getExamSessionId(),
                exam.getCourseCode(),
                exam.getExamDate(),
                exam.getStartTime(),
                exam.getEndTime(),
                exam.getAcademicYear(),
                exam.getSemester(),
                exam.getExamType(),
                exam.getStatus());
    }
}
