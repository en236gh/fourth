package com.backend.fourth.allocation.repository;

import com.backend.fourth.allocation.entity.StudentVenueAllocation;
import com.backend.fourth.allocation.entity.StudentVenueAllocationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentVenueAllocationRepository extends JpaRepository<StudentVenueAllocation, StudentVenueAllocationId> {
    List<StudentVenueAllocation> findByExamSessionId(Integer examSessionId);
    List<StudentVenueAllocation> findByComputerNumber(String computerNumber);
    long countByExamSessionId(Integer examSessionId);
    void deleteByExamSessionId(Integer examSessionId);
    java.util.Optional<StudentVenueAllocation> findByComputerNumberAndExamSessionId(String computerNumber, Integer examSessionId);
    long countByVenueIdAndExamSessionId(Integer venueId, Integer examSessionId);
}
