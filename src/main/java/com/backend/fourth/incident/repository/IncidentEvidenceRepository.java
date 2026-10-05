package com.backend.fourth.incident.repository;

import com.backend.fourth.incident.entity.IncidentEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentEvidenceRepository extends JpaRepository<IncidentEvidence, Integer> {
    List<IncidentEvidence> findByIncidentIncidentIdOrderByUploadedAtAsc(Integer incidentId);

    Optional<IncidentEvidence> findByEvidenceIdAndIncidentIncidentId(Integer evidenceId, Integer incidentId);
}