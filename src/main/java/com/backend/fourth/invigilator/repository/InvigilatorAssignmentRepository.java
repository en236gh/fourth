package com.backend.fourth.invigilator.repository;

import com.backend.fourth.invigilator.entity.InvigilatorAssignment;
import com.backend.fourth.invigilator.entity.InvigilatorAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvigilatorAssignmentRepository extends JpaRepository<InvigilatorAssignment, InvigilatorAssignmentId> {
    List<InvigilatorAssignment> findByStaffId(Integer staffId);

    boolean existsByExamSessionIdAndVenueIdAndStaffId(Integer examSessionId, Integer venueId, Integer staffId);

    List<InvigilatorAssignment> findByStaffIdAndExamSessionId(Integer staffId, Integer examSessionId);
}
