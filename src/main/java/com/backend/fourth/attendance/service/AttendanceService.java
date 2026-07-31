package com.backend.fourth.attendance.service;

import com.backend.fourth.allocation.entity.StudentVenueAllocation;
import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.attendance.dto.AttendanceCheckInResponse;
import com.backend.fourth.attendance.dto.AttendanceSummaryResponse;
import com.backend.fourth.attendance.dto.CheckInRequest;
import com.backend.fourth.attendance.dto.StudentLookupResponse;
import com.backend.fourth.attendance.entity.Attendance;
import com.backend.fourth.attendance.entity.AttendanceStatus;
import com.backend.fourth.attendance.entity.VerificationMethod;
import com.backend.fourth.attendance.repository.AttendanceRepository;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.invigilator.entity.InvigilatorAssignment;
import com.backend.fourth.invigilator.repository.InvigilatorAssignmentRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.staff.repository.StaffRepository;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.repository.StudentRepository;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final ExamSessionRepository examSessionRepository;
    private final VenueRepository venueRepository;
    private final StudentVenueAllocationRepository allocationRepository;
    private final InvigilatorAssignmentRepository assignmentRepository;
    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public StudentLookupResponse lookupStudent(String computerNumber, Integer examSessionId, Staff invigilator) {
        Student student = studentRepository.findByComputerNumber(computerNumber)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));

        StudentVenueAllocation allocation = allocationRepository
                .findByComputerNumberAndExamSessionId(computerNumber, examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Student is not allocated to this examination"));

        if (assignmentRepository.findByStaffIdAndExamSessionId(invigilator.getStaffId(), examSessionId).isEmpty()) {
            throw new IllegalArgumentException("You are not assigned to this examination");
        }

        Venue allocatedVenue = venueRepository.findById(allocation.getVenueId())
                .orElseThrow(() -> new IllegalArgumentException("Allocated venue not found"));

        boolean alreadyCheckedIn = attendanceRepository
                .findByStudentComputerNumberAndExamSessionExamSessionId(computerNumber, examSessionId)
                .isPresent();

        return new StudentLookupResponse(
                student.getComputerNumber(),
                student.getFullName(),
                student.getProgram(),
                student.getPhotoPath(),
                allocatedVenue.getVenueId(),
                allocatedVenue.getVenueName(),
                allocation.getSeatNumber(),
                alreadyCheckedIn);
    }

    @Transactional
    public AttendanceCheckInResponse checkIn(CheckInRequest request, Staff invigilator) {
        if (!assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffId(
                request.examSessionId(), request.venueId(), invigilator.getStaffId())) {
            throw new IllegalArgumentException("You are not assigned to this examination venue");
        }

        Student student = studentRepository.findByComputerNumber(request.computerNumber())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        ExamSession examSession = examSessionRepository.findById(request.examSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
        if ("COMPLETED".equals(examSession.getStatus())) {
            throw new IllegalStateException("Examination has already been completed");
        }
        if (!"IN_PROGRESS".equals(examSession.getStatus())) {
            throw new IllegalStateException("Examination session has not been started");
        }
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        StudentVenueAllocation allocation = allocationRepository
                .findByComputerNumberAndExamSessionId(request.computerNumber(), request.examSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Student is not allocated to this examination"));

        if (attendanceRepository.findByStudentComputerNumberAndExamSessionExamSessionId(
                request.computerNumber(), request.examSessionId()).isPresent()) {
            throw new IllegalStateException("Student has already been checked in for this examination");
        }

        AttendanceStatus status = AttendanceStatus.PRESENT;
        String alert = null;
        if (!allocation.getVenueId().equals(request.venueId())) {
            status = AttendanceStatus.WRONG_VENUE;
            alert = "Student checked in at a venue different from their allocation";
        }

        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setExamSession(examSession);
        attendance.setVenue(venue);
        attendance.setVerifiedBy(invigilator);
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setVerificationMethod(parseVerification(request.verificationMethod()));
        attendance.setAttendanceStatus(status);
        attendance.setScriptsSubmitted(false);
        attendance.setAlertMessage(alert);
        return AttendanceCheckInResponse.from(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    public List<AttendanceCheckInResponse> getAttendanceForExam(Integer examSessionId) {
        examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
        return attendanceRepository.findDetailedByExamSessionId(examSessionId).stream()
                .map(AttendanceCheckInResponse::from)
                .toList();
    }

    @Transactional
    public AttendanceSummaryResponse updateScriptsCollected(Integer examSessionId, Integer count) {
        List<Attendance> attendances = attendanceRepository.findByExamSessionExamSessionId(examSessionId);
        attendances.forEach(attendance -> {
            attendance.setScriptsSubmitted(count > 0);
            attendanceRepository.save(attendance);
        });
        return getAttendanceSummary(examSessionId);
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getAttendanceSummary(Integer examSessionId) {
        examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
        List<Attendance> attendances = attendanceRepository.findByExamSessionExamSessionId(examSessionId);
        long present = attendances.stream()
                .filter(attendance -> attendance.getAttendanceStatus() == AttendanceStatus.PRESENT)
                .count();
        long absent = attendances.stream()
                .filter(attendance -> attendance.getAttendanceStatus() == AttendanceStatus.ABSENT)
                .count();
        long scriptsCollected = attendances.stream()
                .filter(attendance -> Boolean.TRUE.equals(attendance.getScriptsSubmitted()))
                .count();

        return new AttendanceSummaryResponse(present, absent, scriptsCollected, 0);
    }

    /**
     * Marks every allocated student without an attendance row as ABSENT.
     * Called when an exam session is completed (manual end or auto end).
     */
    @Transactional
    public int markAbsenteesForExam(ExamSession examSession, Staff verifiedBy) {
        Integer examSessionId = examSession.getExamSessionId();
        List<StudentVenueAllocation> allocations = allocationRepository.findByExamSessionId(examSessionId);
        Set<String> alreadyRecorded = attendanceRepository.findByExamSessionExamSessionId(examSessionId).stream()
                .map(attendance -> attendance.getStudent() != null ? attendance.getStudent().getComputerNumber() : null)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        LocalDateTime markedAt = LocalDateTime.now();
        int marked = 0;
        for (StudentVenueAllocation allocation : allocations) {
            if (alreadyRecorded.contains(allocation.getComputerNumber())) {
                continue;
            }
            Integer venueId = allocation.getVenueId();
            Staff verifier = verifiedBy != null
                    ? verifiedBy
                    : resolveFallbackVerifier(examSessionId, venueId);

            Attendance absent = new Attendance();
            absent.setStudent(studentRepository.findByComputerNumber(allocation.getComputerNumber()).orElse(null));
            absent.setExamSession(examSession);
            absent.setVenue(venueRepository.findById(venueId).orElse(null));
            absent.setVerifiedBy(verifier);
            absent.setCheckInTime(markedAt);
            absent.setVerificationMethod(VerificationMethod.COMPUTER);
            absent.setAttendanceStatus(AttendanceStatus.ABSENT);
            absent.setScriptsSubmitted(false);
            attendanceRepository.save(absent);
            marked++;
        }
        return marked;
    }

    public long countByStatus(AttendanceStatus status) {
        return attendanceRepository.countByAttendanceStatus(status);
    }

    private Staff resolveFallbackVerifier(Integer examSessionId, Integer venueId) {
        Integer staffId = assignmentRepository.findAll().stream()
                .filter(assignment -> examSessionId.equals(assignment.getExamSessionId())
                        && venueId.equals(assignment.getVenueId()))
                .map(InvigilatorAssignment::getStaffId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No invigilator assigned for auto-absent marking"));
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Invigilator staff not found"));
    }

    private VerificationMethod parseVerification(String value) {
        return switch (value.toUpperCase()) {
            case "COMPUTER" -> VerificationMethod.COMPUTER;
            case "QR_CODE" -> VerificationMethod.QR_CODE;
            case "FACE_RECOGNITION" -> VerificationMethod.FACIAL_RECOGNITION;
            case "QR_AND_FACE" -> VerificationMethod.QR_AND_FACE;
            case "QR_AND_FACIAL" -> VerificationMethod.QR_AND_FACIAL;
            default -> throw new IllegalArgumentException("Unsupported verification method");
        };
    }
}
