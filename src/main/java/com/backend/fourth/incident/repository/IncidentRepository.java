package com.backend.fourth.incident.repository;

import com.backend.fourth.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Integer> {
    @Query("SELECT i FROM Incident i JOIN FETCH i.examSession LEFT JOIN FETCH i.venue WHERE i.incidentId = :incidentId")
    Optional<Incident> findWithExamAndVenueByIncidentId(@Param("incidentId") Integer incidentId);

    List<Incident> findByReportedByStaffIdOrderByOccurredAtDesc(Integer staffId);

    List<Incident> findByExamSessionExamSessionIdInOrderByOccurredAtDesc(List<Integer> examSessionIds);

    @Query("""
            SELECT i FROM Incident i
            JOIN FETCH i.examSession
            LEFT JOIN FETCH i.venue
            LEFT JOIN FETCH i.student
            JOIN FETCH i.reportedBy
            WHERE i.examSession.examSessionId = :examSessionId
            ORDER BY i.occurredAt ASC
            """)
    List<Incident> findDetailedByExamSessionId(@Param("examSessionId") Integer examSessionId);

    List<Incident> findAllByOrderByOccurredAtDesc();

    long countByOccurredAtBetween(LocalDateTime start, LocalDateTime end);

    long countByExamSessionExamSessionIdIn(List<Integer> examSessionIds);
}
