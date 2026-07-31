package com.backend.fourth.venue.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "venue")
@Getter
@Setter
public class Venue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "venue_id")
    private Integer venueId;

    @Column(name = "venue_name", nullable = false)
    private String venueName;

    @Column(name = "building", nullable = false)
    private String building;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;
}
