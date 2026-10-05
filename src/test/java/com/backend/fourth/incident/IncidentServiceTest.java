package com.backend.fourth.incident;

import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.incident.dto.CreateIncidentRequest;
import com.backend.fourth.incident.dto.IncidentEvidenceResponse;
import com.backend.fourth.incident.dto.IncidentResponse;
import com.backend.fourth.incident.entity.Incident;
import com.backend.fourth.incident.entity.IncidentEvidence;
import com.backend.fourth.incident.repository.IncidentRepository;
import com.backend.fourth.incident.service.IncidentEvidenceMetadataService;
import com.backend.fourth.incident.service.IncidentService;
import com.backend.fourth.incident.storage.EvidenceObjectStorage;
import com.backend.fourth.invigilator.repository.InvigilatorAssignmentRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.student.repository.StudentRepository;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private EvidenceObjectStorage evidenceStorage;
    @Mock
    private IncidentEvidenceMetadataService evidenceMetadataService;
    @Mock
    private ExamSessionRepository examSessionRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private InvigilatorAssignmentRepository assignmentRepository;

    @InjectMocks
    private IncidentService incidentService;

    @Test
    void shouldReportIncidentForAssignedInvigilator() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                1, 2, null, "CHEATING", "Phone found under desk", "MAJOR", null);
        Staff staff = new Staff();
        staff.setStaffId(5);
        staff.setFullName("T. Mwewa");

        ExamSession exam = new ExamSession();
        exam.setExamSessionId(1);
        Venue venue = new Venue();
        venue.setVenueId(2);

        when(assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(1, 2, 5, "PUBLISHED")).thenReturn(true);
        when(examSessionRepository.findById(1)).thenReturn(Optional.of(exam));
        when(venueRepository.findById(2)).thenReturn(Optional.of(venue));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            incident.setIncidentId(99);
            return incident;
        });

        IncidentResponse response = incidentService.report(request, staff);

        assertEquals(99, response.incidentId());
        assertEquals("CHEATING", response.incidentType());
        assertEquals("MAJOR", response.severity());
        verify(incidentRepository).save(any(Incident.class));
    }

        @Test
        void shouldUploadValidPngForAssignedExamVenue() throws IOException {
        Staff staff = new Staff();
        staff.setStaffId(5);
        ExamSession exam = new ExamSession();
        exam.setExamSessionId(7);
        Venue venue = new Venue();
        venue.setVenueId(2);
        Incident incident = new Incident();
        incident.setIncidentId(123);
        incident.setExamSession(exam);
        incident.setVenue(venue);
        when(incidentRepository.findWithExamAndVenueByIncidentId(123)).thenReturn(Optional.of(incident));
        when(assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(7, 2, 5, "PUBLISHED"))
            .thenReturn(true);
        when(evidenceMetadataService.save(any(), any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyLong()))
            .thenAnswer(invocation -> {
            IncidentEvidence evidence = new IncidentEvidence();
            evidence.setEvidenceId(45);
            evidence.setIncident(incident);
            evidence.setStorageBucket(invocation.getArgument(2));
            evidence.setObjectPath(invocation.getArgument(3));
            evidence.setOriginalFilename(invocation.getArgument(4));
            evidence.setMimeType(invocation.getArgument(5));
            evidence.setFileSizeBytes(invocation.getArgument(6));
            evidence.setUploadedBy(staff);
            evidence.setUploadedAt(java.time.LocalDateTime.now());
            return evidence;
        });
        ReflectionTestUtils.setField(incidentService, "evidenceBucket", "incident-evidence");

        ByteArrayOutputStream pngOutput = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", pngOutput);
        byte[] png = pngOutput.toByteArray();
        MockMultipartFile file = new MockMultipartFile("file", "../../evidence.png", "image/png", png);

        IncidentEvidenceResponse response = incidentService.uploadEvidence(123, file, staff);

        assertEquals(45, response.evidenceId());
        assertEquals(123, response.incidentId());
        assertEquals("evidence.png", response.originalFilename());
        assertEquals("image/png", response.mimeType());
        verify(evidenceStorage).upload(org.mockito.ArgumentMatchers.eq("incident-evidence"),
            org.mockito.ArgumentMatchers.matches("exams/7/incidents/123/[0-9a-f-]+\\.png"),
            org.mockito.ArgumentMatchers.eq("image/png"), org.mockito.ArgumentMatchers.eq(png));
        }

        @Test
        void shouldRejectFileWithMismatchedDeclaredMimeTypeBeforeUploading() throws IOException {
        Staff staff = new Staff();
        staff.setStaffId(5);
        ExamSession exam = new ExamSession();
        exam.setExamSessionId(7);
        Venue venue = new Venue();
        venue.setVenueId(2);
        Incident incident = new Incident();
        incident.setIncidentId(123);
        incident.setExamSession(exam);
        incident.setVenue(venue);
        when(incidentRepository.findWithExamAndVenueByIncidentId(123)).thenReturn(Optional.of(incident));
        when(assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(7, 2, 5, "PUBLISHED"))
            .thenReturn(true);

        ByteArrayOutputStream pngOutput = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", pngOutput);
        MockMultipartFile file = new MockMultipartFile("file", "evidence.png", "application/pdf", pngOutput.toByteArray());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> incidentService.uploadEvidence(123, file, staff));
        verify(evidenceStorage, never()).upload(any(), any(), any(), any());
        }

        @Test
        void shouldDeleteStorageObjectWhenMetadataSaveFails() throws IOException {
        Staff staff = new Staff();
        staff.setStaffId(5);
        ExamSession exam = new ExamSession();
        exam.setExamSessionId(7);
        Venue venue = new Venue();
        venue.setVenueId(2);
        Incident incident = new Incident();
        incident.setIncidentId(123);
        incident.setExamSession(exam);
        incident.setVenue(venue);
        when(incidentRepository.findWithExamAndVenueByIncidentId(123)).thenReturn(Optional.of(incident));
        when(assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(7, 2, 5, "PUBLISHED"))
            .thenReturn(true);
        when(evidenceMetadataService.save(any(), any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyLong()))
            .thenThrow(new IllegalStateException("database unavailable"));
        ReflectionTestUtils.setField(incidentService, "evidenceBucket", "incident-evidence");

        ByteArrayOutputStream pngOutput = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", pngOutput);
        MockMultipartFile file = new MockMultipartFile("file", "evidence.png", "image/png", pngOutput.toByteArray());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
            () -> incidentService.uploadEvidence(123, file, staff));
        verify(evidenceStorage).delete(org.mockito.ArgumentMatchers.eq("incident-evidence"),
            org.mockito.ArgumentMatchers.matches("exams/7/incidents/123/[0-9a-f-]+\\.png"));
        }
}
