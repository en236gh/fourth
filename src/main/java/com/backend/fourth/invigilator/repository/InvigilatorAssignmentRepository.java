package com.backend.fourth.invigilator.repository;

import com.backend.fourth.invigilator.entity.InvigilatorAssignment;
import com.backend.fourth.invigilator.entity.InvigilatorAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvigilatorAssignmentRepository extends JpaRepository<InvigilatorAssignment, InvigilatorAssignmentId> {
    List<InvigilatorAssignment> findByStaffId(Integer staffId);

    List<InvigilatorAssignment> findByStaffIdAndAssignmentStatusNot(Integer staffId, String assignmentStatus);

    List<InvigilatorAssignment> findByStaffIdAndAssignmentStatus(Integer staffId, String assignmentStatus);

    List<InvigilatorAssignment> findByExamSessionId(Integer examSessionId);

    List<InvigilatorAssignment> findByExamSessionIdAndVenueId(Integer examSessionId, Integer venueId);

    boolean existsByExamSessionIdAndVenueIdAndStaffId(Integer examSessionId, Integer venueId, Integer staffId);

    boolean existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(
            Integer examSessionId, Integer venueId, Integer staffId, String assignmentStatus);

    List<InvigilatorAssignment> findByStaffIdAndExamSessionId(Integer staffId, Integer examSessionId);
}
