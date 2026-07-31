package com.backend.fourth.allocation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "student_venue_allocation")
@IdClass(StudentVenueAllocationId.class)
@Getter
@Setter
public class StudentVenueAllocation {
    @Id
    @Column(name = "computer_number", nullable = false)
    private String computerNumber;

    @Id
    @Column(name = "exam_session_id", nullable = false)
    private Integer examSessionId;

    @Column(name = "venue_id", nullable = false)
    private Integer venueId;

    @Column(name = "seat_number")
    private String seatNumber;
}
