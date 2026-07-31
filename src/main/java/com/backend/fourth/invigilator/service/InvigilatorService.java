package com.backend.fourth.invigilator.service;

import com.backend.fourth.exam.entity.CourseLecturer;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.CourseLecturerRepository;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.exam.service.ExamService;
import com.backend.fourth.invigilator.dto.InvigilatorAssignmentResponse;
import com.backend.fourth.invigilator.dto.LecturerInfoResponse;
import com.backend.fourth.invigilator.entity.InvigilatorAssignment;
import com.backend.fourth.invigilator.repository.InvigilatorAssignmentRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.staff.repository.StaffRepository;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvigilatorService {
    private final InvigilatorAssignmentRepository assignmentRepository;
    private final ExamSessionRepository examSessionRepository;
    private final VenueRepository venueRepository;
    private final CourseLecturerRepository courseLecturerRepository;
    private final StaffRepository staffRepository;
    private final ExamService examService;

    @Transactional(readOnly = true)
    public List<InvigilatorAssignmentResponse> myAssignments(Staff staff) {
        List<InvigilatorAssignmentResponse> responses = new ArrayList<>();
        for (InvigilatorAssignment assignment : assignmentRepository.findByStaffId(staff.getStaffId())) {
            ExamSession exam = examSessionRepository.findById(assignment.getExamSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
            Venue venue = venueRepository.findById(assignment.getVenueId())
                    .orElseThrow(() -> new IllegalArgumentException("Venue not found"));
            responses.add(toResponse(exam, venue));
        }
        return responses;
    }

    @Transactional
    public InvigilatorAssignmentResponse startSession(Staff staff, Integer examSessionId, Integer venueId) {
        if (!assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffId(examSessionId, venueId, staff.getStaffId())) {
            throw new IllegalArgumentException("You are not assigned to this examination venue");
        }
        ExamSession exam = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        if ("COMPLETED".equals(exam.getStatus())) {
            throw new IllegalStateException("Examination has already been completed");
        }
        exam.setStatus("IN_PROGRESS");
        examSessionRepository.save(exam);

        return toResponse(exam, venue);
    }

    @Transactional
    public InvigilatorAssignmentResponse endSession(Staff staff, Integer examSessionId, Integer venueId) {
        if (!assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffId(examSessionId, venueId, staff.getStaffId())) {
            throw new IllegalArgumentException("You are not assigned to this examination venue");
        }
        ExamSession exam = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        if ("COMPLETED".equals(exam.getStatus())) {
            return toResponse(exam, venue);
        }
        if (!"IN_PROGRESS".equals(exam.getStatus())) {
            throw new IllegalStateException("Examination session has not been started");
        }

        examService.completeSession(exam, staff);
        return toResponse(exam, venue);
    }

    private InvigilatorAssignmentResponse toResponse(ExamSession exam, Venue venue) {
        return new InvigilatorAssignmentResponse(
                exam.getExamSessionId(),
                exam.getCourseCode(),
                exam.getExamDate().toString(),
                exam.getStartTime().toString(),
                exam.getEndTime().toString(),
                exam.getStatus(),
                venue.getVenueId(),
                venue.getVenueName(),
                venue.getBuilding(),
                venue.getCapacity(),
                resolveLecturers(exam.getCourseCode()));
    }

    private List<LecturerInfoResponse> resolveLecturers(String courseCode) {
        List<LecturerInfoResponse> lecturers = new ArrayList<>();
        for (CourseLecturer link : courseLecturerRepository.findByCourseCodeOrderByStaffIdAsc(courseCode)) {
            staffRepository.findById(link.getStaffId()).ifPresent(lecturer ->
                    lecturers.add(new LecturerInfoResponse(
                            lecturer.getStaffId(),
                            lecturer.getStaffNo(),
                            lecturer.getFullName(),
                            lecturer.getEmail(),
                            lecturer.getDepartment())));
        }
        return lecturers;
    }
}
