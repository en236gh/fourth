package com.backend.fourth.invigilator.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AssignmentWriteLock {
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.MANDATORY)
    public void acquire() {
        // Serialize API assignment writers so overlapping bulk/manual requests
        // make staffing and conflict decisions from committed assignments.
        jdbc.query("SELECT pg_advisory_xact_lock(741902, 1)", rs -> { });
    }
}
