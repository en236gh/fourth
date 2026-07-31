package com.backend.fourth.exam.repository;

import com.backend.fourth.exam.entity.ExamVenue;
import com.backend.fourth.exam.entity.ExamVenueId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamVenueRepository extends JpaRepository<ExamVenue, ExamVenueId> {
    List<ExamVenue> findByExamSessionIdOrderByVenueIdAsc(Integer examSessionId);
}
