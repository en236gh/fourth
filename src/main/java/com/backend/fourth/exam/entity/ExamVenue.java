package com.backend.fourth.exam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "exam_venue")
@IdClass(ExamVenueId.class)
@Getter
@Setter
public class ExamVenue {
    @Id
    @Column(name = "exam_session_id", nullable = false)
    private Integer examSessionId;

    @Id
    @Column(name = "venue_id", nullable = false)
    private Integer venueId;
}
