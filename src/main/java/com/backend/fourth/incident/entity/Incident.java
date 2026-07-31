package com.backend.fourth.incident.entity;

import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.venue.entity.Venue;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident")
@Getter
@Setter
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "incident_id")
    private Integer incidentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "computer_number")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_staff_id", nullable = false)
    private Staff reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false)
    private IncidentType incidentType;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "severity", nullable = false)
    private String severity = "MINOR";

    @Column(name = "evidence_path")
    private String evidencePath;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
