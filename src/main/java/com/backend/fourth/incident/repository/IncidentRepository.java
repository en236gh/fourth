package com.backend.fourth.incident.repository;

import com.backend.fourth.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Integer> {
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
