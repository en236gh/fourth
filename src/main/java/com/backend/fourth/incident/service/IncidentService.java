package com.backend.fourth.incident.service;

import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.incident.dto.CreateIncidentRequest;
import com.backend.fourth.incident.dto.EvidenceDownloadUrlResponse;
import com.backend.fourth.incident.dto.IncidentEvidenceResponse;
import com.backend.fourth.incident.dto.IncidentResponse;
import com.backend.fourth.incident.entity.Incident;
import com.backend.fourth.incident.entity.IncidentEvidence;
import com.backend.fourth.incident.entity.IncidentType;
import com.backend.fourth.incident.repository.IncidentEvidenceRepository;
import com.backend.fourth.incident.repository.IncidentRepository;
import com.backend.fourth.incident.storage.EvidenceObjectStorage;
import com.backend.fourth.invigilator.entity.InvigilatorAssignment;
import com.backend.fourth.invigilator.repository.InvigilatorAssignmentRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.repository.StudentRepository;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentService {
    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);
    private static final long MAX_EVIDENCE_SIZE = 10L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;
    private static final int MAX_IMAGE_DIMENSION = 12_000;
    private static final Duration DOWNLOAD_URL_VALIDITY = Duration.ofMinutes(5);

    private final IncidentRepository incidentRepository;
    private final IncidentEvidenceRepository evidenceRepository;
    private final ExamSessionRepository examSessionRepository;
    private final VenueRepository venueRepository;
    private final StudentRepository studentRepository;
    private final InvigilatorAssignmentRepository assignmentRepository;
    private final EvidenceObjectStorage evidenceStorage;
    private final IncidentEvidenceMetadataService evidenceMetadataService;

    @Value("${app.storage.s3.bucket:incident-evidence}")
    private String evidenceBucket;

    @Transactional
    public IncidentResponse report(CreateIncidentRequest request, Staff invigilator) {
        if (!assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(
                request.examSessionId(), request.venueId(), invigilator.getStaffId(), "PUBLISHED")) {
            throw new IllegalArgumentException("You are not assigned to this examination venue");
        }

        ExamSession examSession = examSessionRepository.findById(request.examSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new IllegalArgumentException("Venue not found"));

        Student student = null;
        if (request.computerNumber() != null && !request.computerNumber().isBlank()) {
            student = studentRepository.findByComputerNumber(request.computerNumber())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        }

        Incident incident = new Incident();
        incident.setExamSession(examSession);
        incident.setVenue(venue);
        incident.setStudent(student);
        incident.setReportedBy(invigilator);
        incident.setIncidentType(parseType(request.incidentType()));
        incident.setDescription(request.description().trim());
        incident.setSeverity(request.severity() == null || request.severity().isBlank()
                ? "MINOR"
                : request.severity().trim().toUpperCase());
        incident.setEvidencePath(request.evidencePath());
        incident.setOccurredAt(LocalDateTime.now());

        return toResponse(incidentRepository.save(incident));
    }

    public IncidentEvidenceResponse uploadEvidence(Integer incidentId, MultipartFile file, Staff staff) {
        Incident incident = requireAccessibleIncident(incidentId, staff);
        ValidatedFile validated = validateFile(file);
        String objectPath = "exams/" + incident.getExamSession().getExamSessionId()
                + "/incidents/" + incidentId + "/" + UUID.randomUUID() + "." + validated.extension();
        evidenceStorage.upload(evidenceBucket, objectPath, validated.mimeType(), validated.content());

        IncidentEvidence saved;
        try {
            saved = evidenceMetadataService.save(
                    incident, staff, evidenceBucket, objectPath,
                    validated.originalFilename(), validated.mimeType(), validated.content().length);
        } catch (RuntimeException databaseFailure) {
            removeUnlinkedObject(evidenceBucket, objectPath);
            throw databaseFailure;
        }
        return toEvidenceResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidentEvidenceResponse> listEvidence(Integer incidentId, Staff staff) {
        requireAccessibleIncident(incidentId, staff);
        return evidenceRepository.findByIncidentIncidentIdOrderByUploadedAtAsc(incidentId).stream()
                .map(this::toEvidenceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EvidenceDownloadUrlResponse createEvidenceDownloadUrl(Integer incidentId, Integer evidenceId, Staff staff) {
        requireAccessibleIncident(incidentId, staff);
        IncidentEvidence evidence = evidenceRepository.findByEvidenceIdAndIncidentIncidentId(evidenceId, incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence attachment not found"));
        Instant expiresAt = Instant.now().plus(DOWNLOAD_URL_VALIDITY);
        String url = evidenceStorage.createDownloadUrl(
                evidence.getStorageBucket(), evidence.getObjectPath(), DOWNLOAD_URL_VALIDITY);
        return new EvidenceDownloadUrlResponse(url, expiresAt);
    }

    private Incident requireAccessibleIncident(Integer incidentId, Staff staff) {
        Incident incident = incidentRepository.findWithExamAndVenueByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));
        boolean administrator = staff.getRoles() != null && staff.getRoles().stream()
                .anyMatch(role -> "ADMINISTRATOR".equals(role.getName()));
        boolean assigned = incident.getVenue() != null
                && incident.getExamSession().isSchedulePublished()
                && assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(
                incident.getExamSession().getExamSessionId(), incident.getVenue().getVenueId(),
                staff.getStaffId(), "PUBLISHED");
        if (!administrator && !assigned) {
            throw new AccessDeniedException("You are not assigned to this incident's published examination venue");
        }
        return incident;
    }

    private ValidatedFile validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A non-empty file is required");
        }
        if (file.getSize() > MAX_EVIDENCE_SIZE) {
            throw new IllegalArgumentException("Evidence files must not exceed 10 MB");
        }
        try {
            byte[] content = file.getBytes();
            if (content.length > MAX_EVIDENCE_SIZE) {
                throw new IllegalArgumentException("Evidence files must not exceed 10 MB");
            }
            FileType fileType = detectFileType(content);
            String declaredType = file.getContentType();
            if (declaredType != null && !declaredType.isBlank()
                    && !fileType.mimeType().equalsIgnoreCase(declaredType.trim())) {
                throw new IllegalArgumentException("The uploaded file content does not match its declared MIME type");
            }
            return new ValidatedFile(content, fileType.mimeType(), fileType.extension(), safeFilename(file.getOriginalFilename(), fileType.extension()));
        } catch (IOException exception) {
            throw new IllegalArgumentException("The uploaded file could not be read", exception);
        }
    }

    private FileType detectFileType(byte[] content) throws IOException {
        if (content.length >= 5 && new String(content, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
            try (PDDocument document = Loader.loadPDF(content)) {
                if (document.getNumberOfPages() > 0) return new FileType("application/pdf", "pdf");
            } catch (IOException | RuntimeException invalidPdf) {
                throw new IllegalArgumentException("The uploaded PDF file is invalid", invalidPdf);
            }
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input != null) {
                var readers = ImageIO.getImageReaders(input);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(input, true, true);
                        int width = reader.getWidth(0);
                        int height = reader.getHeight(0);
                        if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION
                                || (long) width * height > MAX_IMAGE_PIXELS) {
                            throw new IllegalArgumentException("Image dimensions exceed the supported limits");
                        }
                        var image = reader.read(0);
                        String format = reader.getFormatName();
                        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                            throw new IllegalArgumentException("The uploaded image file is invalid");
                        }
                        if ("png".equalsIgnoreCase(format)) return new FileType("image/png", "png");
                        if ("jpeg".equalsIgnoreCase(format) || "jpg".equalsIgnoreCase(format)) {
                            return new FileType("image/jpeg", "jpg");
                        }
                    } finally {
                        reader.dispose();
                    }
                }
            }
        } catch (IOException invalidImage) {
            throw new IllegalArgumentException("The uploaded image file is invalid", invalidImage);
        }
        throw new IllegalArgumentException("Only valid JPEG, PNG, and PDF evidence files are accepted");
    }

    private String safeFilename(String providedName, String extension) {
        String filename = providedName == null ? "evidence." + extension : providedName;
        int separator = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        filename = filename.substring(separator + 1).replaceAll("[\\p{Cntrl}]", "_").trim();
        if (filename.isBlank() || filename.equals(".") || filename.equals("..")) filename = "evidence." + extension;
        if (filename.length() > 255) filename = filename.substring(filename.length() - 255);
        return filename;
    }

    private void removeUnlinkedObject(String bucket, String objectPath) {
        try {
            evidenceStorage.delete(bucket, objectPath);
        } catch (RuntimeException cleanupFailure) {
            log.error("Failed to remove unlinked incident evidence object from private storage");
        }
    }

    private IncidentEvidenceResponse toEvidenceResponse(IncidentEvidence evidence) {
        return new IncidentEvidenceResponse(
                evidence.getEvidenceId(),
                evidence.getIncident().getIncidentId(),
                evidence.getStorageBucket(),
                evidence.getObjectPath(),
                evidence.getOriginalFilename(),
                evidence.getMimeType(),
                evidence.getFileSizeBytes(),
                evidence.getUploadedBy().getStaffId(),
                evidence.getUploadedAt());
    }

    private record FileType(String mimeType, String extension) { }

    private record ValidatedFile(byte[] content, String mimeType, String extension, String originalFilename) { }

    @Transactional(readOnly = true)
    public List<IncidentResponse> listForAdmin() {
        return incidentRepository.findAllByOrderByOccurredAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> listForInvigilator(Staff invigilator) {
        List<Integer> examIds = assignmentRepository.findByStaffId(invigilator.getStaffId()).stream()
                .map(InvigilatorAssignment::getExamSessionId)
                .distinct()
                .toList();
        if (examIds.isEmpty()) {
            return List.of();
        }
        return incidentRepository.findByExamSessionExamSessionIdInOrderByOccurredAtDesc(examIds).stream()
                .map(this::toResponse)
                .toList();
    }

    public long countAll() {
        return incidentRepository.count();
    }

    private IncidentType parseType(String value) {
        try {
            return IncidentType.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unsupported incident type: " + value);
        }
    }

    private IncidentResponse toResponse(Incident incident) {
        return new IncidentResponse(
                incident.getIncidentId(),
                incident.getExamSession() != null ? incident.getExamSession().getExamSessionId() : null,
                incident.getVenue() != null ? incident.getVenue().getVenueId() : null,
                incident.getStudent() != null ? incident.getStudent().getComputerNumber() : null,
                incident.getStudent() != null ? incident.getStudent().getFullName() : null,
                incident.getIncidentType() != null ? incident.getIncidentType().name() : null,
                incident.getDescription(),
                incident.getSeverity(),
                incident.getEvidencePath(),
                incident.getReportedBy() != null ? incident.getReportedBy().getStaffId() : null,
                incident.getReportedBy() != null ? incident.getReportedBy().getFullName() : null,
                incident.getOccurredAt()
        );
    }
}
