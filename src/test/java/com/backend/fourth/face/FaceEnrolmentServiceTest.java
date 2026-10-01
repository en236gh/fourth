package com.backend.fourth.face;

import com.backend.fourth.face.client.FaceEmbedding;
import com.backend.fourth.face.client.FacePurpose;
import com.backend.fourth.face.client.FaceServiceClient;
import com.backend.fourth.face.dto.FaceEnrolmentResponse;
import com.backend.fourth.face.entity.StudentFaceTemplate;
import com.backend.fourth.face.repository.StudentFaceTemplateRepository;
import com.backend.fourth.face.service.FaceEnrolmentService;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FaceEnrolmentServiceTest {
    private static final String STUDENT = "2022004264";
    private static final byte[] PHOTO = {9, 9};

    @Mock private StudentRepository studentRepository;
    @Mock private StudentFaceTemplateRepository templateRepository;
    @Mock private FaceServiceClient faceServiceClient;
    @InjectMocks private FaceEnrolmentService service;

    @Test
    void enrolmentRequiresRecordedConsent() {
        assertThrows(IllegalArgumentException.class, () -> service.enrol(STUDENT, PHOTO, "image/jpeg", false, admin()));
        verifyNoInteractions(faceServiceClient, templateRepository);
    }

    @Test
    void unknownStudentIsRejectedBeforeInference() {
        when(studentRepository.findByComputerNumber(STUDENT)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.enrol(STUDENT, PHOTO, "image/jpeg", true, admin()));
        verifyNoInteractions(faceServiceClient);
    }

    @Test
    void firstEnrolmentStoresOnlyTheEmbedding() {
        when(studentRepository.findByComputerNumber(STUDENT)).thenReturn(Optional.of(new Student()));
        float[] embedding = new float[512];
        embedding[3] = 1f;
        when(faceServiceClient.embed(PHOTO, "image/jpeg", FacePurpose.ENROLMENT))
                .thenReturn(new FaceEmbedding("buffalo_l", embedding, 0.91, 1));
        when(templateRepository.findById(STUDENT)).thenReturn(Optional.empty());
        when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FaceEnrolmentResponse response = service.enrol(STUDENT, PHOTO, "image/jpeg", true, admin());

        ArgumentCaptor<StudentFaceTemplate> saved = ArgumentCaptor.forClass(StudentFaceTemplate.class);
        verify(templateRepository).save(saved.capture());
        assertArrayEquals(embedding, saved.getValue().getEmbedding());
        assertEquals("buffalo_l", saved.getValue().getModelName());
        assertEquals(7, saved.getValue().getEnrolledByStaffId());
        assertTrue(response.enrolled());
        assertEquals(0.91f, response.detectionScore(), 1e-6);
    }

    @Test
    void reEnrolmentReplacesEmbeddingButKeepsOriginalEnrolmentDate() {
        when(studentRepository.findByComputerNumber(STUDENT)).thenReturn(Optional.of(new Student()));
        StudentFaceTemplate existing = new StudentFaceTemplate();
        existing.setComputerNumber(STUDENT);
        existing.setEnrolledAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        existing.setEmbedding(new float[512]);
        when(templateRepository.findById(STUDENT)).thenReturn(Optional.of(existing));
        float[] replacement = new float[512];
        replacement[0] = 1f;
        when(faceServiceClient.embed(any(), any(), any())).thenReturn(new FaceEmbedding("buffalo_l", replacement, 0.8, 1));
        when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FaceEnrolmentResponse response = service.enrol(STUDENT, PHOTO, "image/png", true, admin());

        assertEquals(LocalDateTime.of(2026, 1, 1, 9, 0), response.enrolledAt());
        assertArrayEquals(replacement, existing.getEmbedding());
    }

    @Test
    void statusReportsMissingEnrolment() {
        when(studentRepository.findByComputerNumber(STUDENT)).thenReturn(Optional.of(new Student()));
        when(templateRepository.findById(STUDENT)).thenReturn(Optional.empty());
        assertFalse(service.status(STUDENT).enrolled());
    }

    private static Staff admin() {
        Staff staff = new Staff();
        staff.setStaffId(7);
        return staff;
    }
}
