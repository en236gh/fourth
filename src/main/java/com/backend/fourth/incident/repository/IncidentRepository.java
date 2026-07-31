package com.backend.fourth.incident.repository;

import com.backend.fourth.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Integer> {
    List<Incident> findByReportedByStaffIdOrderByOccurredAtDesc(Integer staffId);

    List<Incident> findByExamSessionExamSessionIdInOrderByOccurredAtDesc(List<Integer> examSessionIds);

    List<Incident> findAllByOrderByOccurredAtDesc();

    long countByOccurredAtBetween(LocalDateTime start, LocalDateTime end);

    long countByExamSessionExamSessionIdIn(List<Integer> examSessionIds);
}
