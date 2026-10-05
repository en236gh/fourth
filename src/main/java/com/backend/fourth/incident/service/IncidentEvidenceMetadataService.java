package com.backend.fourth.incident.service;

import com.backend.fourth.incident.entity.Incident;
import com.backend.fourth.incident.entity.IncidentEvidence;
import com.backend.fourth.incident.repository.IncidentEvidenceRepository;
import com.backend.fourth.staff.entity.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IncidentEvidenceMetadataService {
    private final IncidentEvidenceRepository evidenceRepository;

    @Transactional
    public IncidentEvidence save(
            Incident incident,
            Staff staff,
            String bucket,
            String objectPath,
            String originalFilename,
            String mimeType,
            long sizeBytes) {
        IncidentEvidence evidence = new IncidentEvidence();
        evidence.setIncident(incident);
        evidence.setStorageBucket(bucket);
        evidence.setObjectPath(objectPath);
        evidence.setOriginalFilename(originalFilename);
        evidence.setMimeType(mimeType);
        evidence.setFileSizeBytes(sizeBytes);
        evidence.setUploadedBy(staff);
        evidence.setUploadedAt(LocalDateTime.now());
        return evidenceRepository.saveAndFlush(evidence);
    }
}