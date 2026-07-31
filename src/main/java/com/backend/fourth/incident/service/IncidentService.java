package com.backend.fourth.incident.service;

import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.incident.dto.CreateIncidentRequest;
import com.backend.fourth.incident.dto.IncidentResponse;
import com.backend.fourth.incident.entity.Incident;
import com.backend.fourth.incident.entity.IncidentType;
import com.backend.fourth.incident.repository.IncidentRepository;
import com.backend.fourth.invigilator.entity.InvigilatorAssignment;
import com.backend.fourth.invigilator.repository.InvigilatorAssignmentRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.repository.StudentRepository;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final ExamSessionRepository examSessionRepository;
    private final VenueRepository venueRepository;
    private final StudentRepository studentRepository;
    private final InvigilatorAssignmentRepository assignmentRepository;

    @Transactional
    public IncidentResponse report(CreateIncidentRequest request, Staff invigilator) {
        if (!assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffId(
                request.examSessionId(), request.venueId(), invigilator.getStaffId())) {
            throw new IllegalArgumentException("You are not assigned to this examination venue");
        }

        ExamSession examSession = examSessionRepository.findById(request.examSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        Student student = null;
        if (request.computerNumber() != null && !request.computerNumber().isBlank()) {
            student = studentRepository.findByComputerNumber(request.computerNumber())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        }

        Incident incident = new Incident();
        incident.setExamSession(examSession);
        incident.setVenue(venue);
        incident.setStudent(student);
        incident.setReportedBy(invigilator);
        incident.setIncidentType(parseType(request.incidentType()));
        incident.setDescription(request.description().trim());
        incident.setSeverity(request.severity() == null || request.severity().isBlank()
                ? "MINOR"
                : request.severity().trim().toUpperCase());
        incident.setEvidencePath(request.evidencePath());
        incident.setOccurredAt(LocalDateTime.now());

        return toResponse(incidentRepository.save(incident));
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> listForAdmin() {
        return incidentRepository.findAllByOrderByOccurredAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> listForInvigilator(Staff invigilator) {
        List<Integer> examIds = assignmentRepository.findByStaffId(invigilator.getStaffId()).stream()
                .map(InvigilatorAssignment::getExamSessionId)
                .distinct()
                .toList();
        if (examIds.isEmpty()) {
            return List.of();
        }
        return incidentRepository.findByExamSessionExamSessionIdInOrderByOccurredAtDesc(examIds).stream()
                .map(this::toResponse)
                .toList();
    }

    public long countAll() {
        return incidentRepository.count();
    }

    private IncidentType parseType(String value) {
        try {
            return IncidentType.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unsupported incident type: " + value);
        }
    }

    private IncidentResponse toResponse(Incident incident) {
        return new IncidentResponse(
                incident.getIncidentId(),
                incident.getExamSession() != null ? incident.getExamSession().getExamSessionId() : null,
                incident.getVenue() != null ? incident.getVenue().getVenueId() : null,
                incident.getStudent() != null ? incident.getStudent().getComputerNumber() : null,
                incident.getStudent() != null ? incident.getStudent().getFullName() : null,
                incident.getIncidentType() != null ? incident.getIncidentType().name() : null,
                incident.getDescription(),
                incident.getSeverity(),
                incident.getEvidencePath(),
                incident.getReportedBy() != null ? incident.getReportedBy().getStaffId() : null,
                incident.getReportedBy() != null ? incident.getReportedBy().getFullName() : null,
                incident.getOccurredAt()
        );
    }
}
