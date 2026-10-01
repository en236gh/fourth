package com.backend.fourth.attendance.service;

import com.backend.fourth.attendance.dto.AttendanceCheckInResponse;
import com.backend.fourth.attendance.dto.FaceCheckInResponse;
import com.backend.fourth.attendance.dto.FaceCheckInResponse.Outcome;
import com.backend.fourth.face.client.FaceEmbedding;
import com.backend.fourth.face.client.FacePurpose;
import com.backend.fourth.face.client.FaceServiceClient;
import com.backend.fourth.face.entity.StudentFaceTemplate;
import com.backend.fourth.face.repository.StudentFaceTemplateRepository;
import com.backend.fourth.face.service.FaceMatcher;
import com.backend.fourth.incident.service.IncidentService;
import com.backend.fourth.staff.entity.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * QR + face check-in. The QR identifies who the student claims to be; the face confirms it (1:1 verification).
 * Deliberately not @Transactional: no database connection is held while the face service runs inference.
 */
@Service
@RequiredArgsConstructor
public class FaceCheckInService {
    private final AttendanceService attendanceService;
    private final StudentFaceTemplateRepository templateRepository;
    private final FaceServiceClient faceServiceClient;
    private final FaceMatcher faceMatcher;
    private final IncidentService incidentService;

    public FaceCheckInResponse checkIn(String qrToken, Integer examSessionId, Integer venueId,
                                       byte[] image, String contentType, String overrideReason, Staff invigilator) {
        String computerNumber = attendanceService.resolveComputerNumberFromQr(qrToken, examSessionId);
        // Fail fast on eligibility before spending time on inference.
        attendanceService.validateCheckIn(computerNumber, examSessionId, venueId, invigilator);

        StudentFaceTemplate template = templateRepository.findById(computerNumber).orElse(null);
        if (template == null) {
            return response(Outcome.NOT_ENROLLED, computerNumber, null, null, null,
                    "This student has no enrolled face. Check them in with the QR code and refer them for enrolment.");
        }

        FaceEmbedding probe = faceServiceClient.embed(image, contentType, FacePurpose.VERIFICATION);
        if (!template.getModelName().equals(probe.model())) {
            throw new IllegalStateException("The student's face was enrolled with model " + template.getModelName()
                    + " but the face service now runs " + probe.model() + ". Re-enrol the student.");
        }

        double rawSimilarity = FaceMatcher.similarity(template.getEmbedding(), probe.embedding());
        double similarity = round(rawSimilarity);
        String reason = overrideReason == null || overrideReason.isBlank() ? null : overrideReason.trim();

        return switch (faceMatcher.decide(rawSimilarity)) {
            case MATCH -> response(Outcome.VERIFIED, computerNumber, similarity,
                    record(computerNumber, examSessionId, venueId, invigilator, similarity, null, null), null,
                    "Face verified. Attendance recorded.");
            case REVIEW -> reason == null
                    ? response(Outcome.REVIEW_REQUIRED, computerNumber, similarity, null, null,
                            "Face match is uncertain. Retake the photo, or compare the student with their ID and confirm with a reason.")
                    : response(Outcome.CONFIRMED_BY_INVIGILATOR, computerNumber, similarity,
                            record(computerNumber, examSessionId, venueId, invigilator, similarity, reason,
                                    String.format(Locale.ROOT,
                                            "Face similarity %.2f below automatic threshold %.2f; identity confirmed by invigilator: %s",
                                            similarity, faceMatcher.matchThreshold(), reason)),
                            null, "Identity confirmed by invigilator. Attendance recorded.");
            case REJECT -> {
                Integer incidentId = incidentService.recordSuspectedImpersonation(
                        examSessionId, venueId, computerNumber, invigilator, String.format(Locale.ROOT,
                                "Face verification failed at check-in: similarity %.2f is below the rejection threshold %.2f. "
                                        + "The person presenting this examination pass may not be the registered student.",
                                similarity, faceMatcher.reviewThreshold()));
                yield response(Outcome.REJECTED, computerNumber, similarity, null, incidentId,
                        "Face does not match the registered student. Attendance not recorded; an impersonation incident was raised.");
            }
        };
    }

    private AttendanceCheckInResponse record(String computerNumber, Integer examSessionId, Integer venueId,
                                             Staff invigilator, double similarity, String reason, String alert) {
        return attendanceService.checkInWithFace(
                computerNumber, examSessionId, venueId, invigilator, (float) similarity, reason, alert);
    }

    private FaceCheckInResponse response(Outcome outcome, String computerNumber, Double similarity,
                                         AttendanceCheckInResponse attendance, Integer incidentId, String message) {
        return new FaceCheckInResponse(outcome, computerNumber, similarity,
                faceMatcher.matchThreshold(), faceMatcher.reviewThreshold(), attendance, incidentId, message);
    }

    private static double round(double value) {
        return Math.round(value * 10_000d) / 10_000d;
    }
}
