package com.backend.fourth.venue.repository;

import com.backend.fourth.venue.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Integer> {
}
