package com.backend.fourth.attendance;

import com.backend.fourth.attendance.dto.AttendanceCheckInResponse;
import com.backend.fourth.attendance.dto.FaceCheckInResponse;
import com.backend.fourth.attendance.dto.FaceCheckInResponse.Outcome;
import com.backend.fourth.attendance.service.AttendanceService;
import com.backend.fourth.attendance.service.FaceCheckInService;
import com.backend.fourth.face.FaceProperties;
import com.backend.fourth.face.client.FaceEmbedding;
import com.backend.fourth.face.client.FacePhotoRejectedException;
import com.backend.fourth.face.client.FacePurpose;
import com.backend.fourth.face.client.FaceServiceClient;
import com.backend.fourth.face.entity.StudentFaceTemplate;
import com.backend.fourth.face.repository.StudentFaceTemplateRepository;
import com.backend.fourth.face.service.FaceMatcher;
import com.backend.fourth.incident.service.IncidentService;
import com.backend.fourth.staff.entity.Staff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaceCheckInServiceTest {
    private static final String STUDENT = "2022004264";
    private static final byte[] PHOTO = {1, 2, 3};

    @Mock private AttendanceService attendanceService;
    @Mock private StudentFaceTemplateRepository templateRepository;
    @Mock private FaceServiceClient faceServiceClient;
    @Mock private IncidentService incidentService;

    private FaceCheckInService service;
    private final Staff invigilator = new Staff();

    @BeforeEach
    void setUp() {
        invigilator.setStaffId(2);
        service = new FaceCheckInService(attendanceService, templateRepository, faceServiceClient,
                new FaceMatcher(new FaceProperties()), incidentService);
        when(attendanceService.resolveComputerNumberFromQr("qr", 5)).thenReturn(STUDENT);
    }

    @Test
    void matchingFaceRecordsAttendanceWithScore() {
        enrolled(unit(0));
        probe(similarTo(0, 0.8));
        AttendanceCheckInResponse recorded = attendance();
        when(attendanceService.checkInWithFace(eq(STUDENT), eq(5), eq(16), eq(invigilator), anyFloat(), isNull(), isNull()))
                .thenReturn(recorded);

        FaceCheckInResponse response = checkIn(null);

        assertEquals(Outcome.VERIFIED, response.outcome());
        assertEquals(0.8, response.similarity(), 1e-4);
        assertSame(recorded, response.attendance());
        verify(faceServiceClient).embed(PHOTO, "image/jpeg", FacePurpose.VERIFICATION);
        verifyNoInteractions(incidentService);
    }

    @Test
    void uncertainFaceWithoutReasonRecordsNothing() {
        enrolled(unit(0));
        probe(similarTo(0, 0.38));

        FaceCheckInResponse response = checkIn("   ");

        assertEquals(Outcome.REVIEW_REQUIRED, response.outcome());
        assertNull(response.attendance());
        verify(attendanceService, never()).checkInWithFace(any(), any(), any(), any(), anyFloat(), any(), any());
        verifyNoInteractions(incidentService);
    }

    @Test
    void uncertainFaceWithInvigilatorReasonIsRecordedAndFlagged() {
        enrolled(unit(0));
        probe(similarTo(0, 0.38));
        when(attendanceService.checkInWithFace(eq(STUDENT), eq(5), eq(16), eq(invigilator), anyFloat(),
                eq("Checked NRC, new glasses"), contains("identity confirmed by invigilator: Checked NRC, new glasses")))
                .thenReturn(attendance());

        FaceCheckInResponse response = checkIn("  Checked NRC, new glasses ");

        assertEquals(Outcome.CONFIRMED_BY_INVIGILATOR, response.outcome());
        assertTrue(response.attendance() != null);
    }

    @Test
    void clearMismatchRaisesImpersonationIncidentEvenWhenReasonGiven() {
        enrolled(unit(0));
        probe(unit(1));
        when(incidentService.recordSuspectedImpersonation(eq(5), eq(16), eq(STUDENT), eq(invigilator), contains("0.00")))
                .thenReturn(77);

        FaceCheckInResponse response = checkIn("Looks like them to me");

        assertEquals(Outcome.REJECTED, response.outcome());
        assertEquals(77, response.incidentId());
        verify(attendanceService, never()).checkInWithFace(any(), any(), any(), any(), anyFloat(), any(), any());
    }

    @Test
    void studentWithoutEnrolmentIsReportedWithoutCallingTheModel() {
        when(templateRepository.findById(STUDENT)).thenReturn(Optional.empty());

        assertEquals(Outcome.NOT_ENROLLED, checkIn(null).outcome());
        verifyNoInteractions(faceServiceClient, incidentService);
    }

    @Test
    void ineligibleCheckInFailsBeforeInference() {
        when(attendanceService.validateCheckIn(STUDENT, 5, 16, invigilator))
                .thenThrow(new IllegalArgumentException("You are not assigned to this examination venue"));

        assertThrows(IllegalArgumentException.class, () -> checkIn(null));
        verifyNoInteractions(templateRepository, faceServiceClient, incidentService);
    }

    @Test
    void unusablePhotoPropagatesSoTheInvigilatorCanRetake() {
        enrolled(unit(0));
        when(faceServiceClient.embed(any(), anyString(), any()))
                .thenThrow(new FacePhotoRejectedException("NO_FACE", "No face was detected."));

        assertThrows(FacePhotoRejectedException.class, () -> checkIn(null));
        verifyNoInteractions(incidentService);
    }

    @Test
    void templateFromAnotherModelIsNotCompared() {
        StudentFaceTemplate template = enrolled(unit(0));
        template.setModelName("antelopev2");
        probe(unit(0));

        assertThrows(IllegalStateException.class, () -> checkIn(null));
        verify(attendanceService, never()).checkInWithFace(any(), any(), any(), any(), anyFloat(), any(), any());
    }

    private FaceCheckInResponse checkIn(String reason) {
        return service.checkIn("qr", 5, 16, PHOTO, "image/jpeg", reason, invigilator);
    }

    private StudentFaceTemplate enrolled(float[] embedding) {
        StudentFaceTemplate template = new StudentFaceTemplate();
        template.setComputerNumber(STUDENT);
        template.setEmbedding(embedding);
        template.setModelName("buffalo_l");
        when(templateRepository.findById(STUDENT)).thenReturn(Optional.of(template));
        return template;
    }

    private void probe(float[] embedding) {
        when(faceServiceClient.embed(any(), anyString(), any()))
                .thenReturn(new FaceEmbedding("buffalo_l", embedding, 0.9, 1));
    }

    private static float[] unit(int axis) {
        float[] vector = new float[512];
        vector[axis] = 1f;
        return vector;
    }

    /** A unit vector whose cosine similarity with unit(axis) is exactly `cosine`. */
    private static float[] similarTo(int axis, double cosine) {
        float[] vector = new float[512];
        vector[axis] = (float) cosine;
        vector[axis + 1] = (float) Math.sqrt(1 - cosine * cosine);
        return vector;
    }

    private static AttendanceCheckInResponse attendance() {
        return new AttendanceCheckInResponse(1, STUDENT, "K. Banda", 5, 16, "Main LT 1", 2, "Invigilator",
                null, null, null, false, null, 0.8f);
    }
}
