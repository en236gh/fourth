package com.backend.fourth.attendance;

import com.backend.fourth.attendance.controller.AttendanceController;
import com.backend.fourth.attendance.dto.FaceCheckInResponse;
import com.backend.fourth.attendance.dto.FaceCheckInResponse.Outcome;
import com.backend.fourth.attendance.service.AttendanceService;
import com.backend.fourth.attendance.service.FaceCheckInService;
import com.backend.fourth.common.exception.GlobalExceptionHandler;
import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.face.client.FacePhotoRejectedException;
import com.backend.fourth.face.client.FaceServiceUnavailableException;
import com.backend.fourth.scheduling.SchedulingDeniedAudit;
import com.backend.fourth.staff.entity.Staff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FaceCheckInControllerTest {
    private final FaceCheckInService faceCheckInService = mock(FaceCheckInService.class);
    private final CurrentStaffResolver staffResolver = mock(CurrentStaffResolver.class);
    private final Staff invigilator = new Staff();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        invigilator.setStaffId(2);
        when(staffResolver.requireCurrentStaff()).thenReturn(invigilator);
        mvc = MockMvcBuilders
                .standaloneSetup(new AttendanceController(mock(AttendanceService.class), faceCheckInService, staffResolver))
                .setControllerAdvice(new GlobalExceptionHandler(mock(SchedulingDeniedAudit.class)))
                .build();
    }

    @Test
    void multipartFormIsBoundAndOutcomeReturned() throws Exception {
        when(faceCheckInService.checkIn(eq("qr-token"), eq(5), eq(16), any(), eq("image/jpeg"), eq("Checked NRC"), eq(invigilator)))
                .thenReturn(new FaceCheckInResponse(Outcome.VERIFIED, "2022004264", 0.71, 0.45, 0.30, null, null, "Face verified."));

        mvc.perform(request().param("overrideReason", "Checked NRC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.outcome").value("VERIFIED"))
                .andExpect(jsonPath("$.data.similarity").value(0.71));
        verify(faceCheckInService).checkIn(eq("qr-token"), eq(5), eq(16), eq(new byte[]{1, 2, 3}), eq("image/jpeg"),
                eq("Checked NRC"), eq(invigilator));
    }

    @Test
    void missingImageIsAValidationError() throws Exception {
        mvc.perform(multipart("/api/attendance/check-in-by-qr-and-face")
                        .param("qrToken", "qr-token").param("examSessionId", "5").param("venueId", "16"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(faceCheckInService);
    }

    @Test
    void unusablePhotoIs422() throws Exception {
        when(faceCheckInService.checkIn(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new FacePhotoRejectedException("NO_FACE", "No face was detected."));
        mvc.perform(request())
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("No face was detected."));
    }

    @Test
    void faceServiceOutageIs503WithQrFallbackAdvice() throws Exception {
        when(faceCheckInService.checkIn(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new FaceServiceUnavailableException("down", null));
        mvc.perform(request())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(
                        "Face verification is temporarily unavailable. Check the student in with the QR code instead."));
    }

    private MockMultipartHttpServletRequestBuilder request() {
        return multipart("/api/attendance/check-in-by-qr-and-face")
                .file(new MockMultipartFile("image", "capture.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                .param("qrToken", "qr-token")
                .param("examSessionId", "5")
                .param("venueId", "16");
    }
}
