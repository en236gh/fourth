package com.backend.fourth.invigilator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "invigilator_assignment")
@IdClass(InvigilatorAssignmentId.class)
@Getter
@Setter
public class InvigilatorAssignment {
    @Id
    @Column(name = "exam_session_id", nullable = false)
    private Integer examSessionId;

    @Id
    @Column(name = "venue_id", nullable = false)
    private Integer venueId;

    @Id
    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "assignment_status", nullable = false)
    private String assignmentStatus = "DRAFT";

    @Column(name = "assigned_by_staff_id")
    private Integer assignedByStaffId;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "assignment_notes")
    private String assignmentNotes;
}
