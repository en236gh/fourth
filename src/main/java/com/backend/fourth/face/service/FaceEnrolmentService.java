package com.backend.fourth.face.service;

import com.backend.fourth.face.client.FaceEmbedding;
import com.backend.fourth.face.client.FacePurpose;
import com.backend.fourth.face.client.FaceServiceClient;
import com.backend.fourth.face.dto.FaceEnrolmentResponse;
import com.backend.fourth.face.entity.StudentFaceTemplate;
import com.backend.fourth.face.repository.StudentFaceTemplateRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Enrolment is administrator-only and done in person against the student's ID: if students could enrol
 * themselves, someone sitting an exam for a friend could simply enrol their own face on the friend's account.
 */
@Service
@RequiredArgsConstructor
public class FaceEnrolmentService {
    private final StudentRepository studentRepository;
    private final StudentFaceTemplateRepository templateRepository;
    private final FaceServiceClient faceServiceClient;

    /** Not @Transactional: inference runs before any database write. Re-enrolling replaces the template. */
    public FaceEnrolmentResponse enrol(String computerNumber, byte[] image, String contentType,
                                       boolean consentConfirmed, Staff administrator) {
        if (!consentConfirmed) {
            throw new IllegalArgumentException(
                    "Record the student's consent to biometric verification before enrolling their face");
        }
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("A face photo is required");
        }
        studentRepository.findByComputerNumber(computerNumber)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        FaceEmbedding embedding = faceServiceClient.embed(image, contentType, FacePurpose.ENROLMENT);

        LocalDateTime now = LocalDateTime.now();
        StudentFaceTemplate template = templateRepository.findById(computerNumber).orElseGet(() -> {
            StudentFaceTemplate created = new StudentFaceTemplate();
            created.setComputerNumber(computerNumber);
            created.setEnrolledAt(now);
            return created;
        });
        template.setEmbedding(embedding.embedding());
        template.setModelName(embedding.model());
        template.setDetectionScore((float) embedding.detScore());
        template.setConsentRecordedAt(now);
        template.setEnrolledByStaffId(administrator.getStaffId());
        template.setUpdatedAt(now);
        return FaceEnrolmentResponse.from(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public FaceEnrolmentResponse status(String computerNumber) {
        studentRepository.findByComputerNumber(computerNumber)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        return templateRepository.findById(computerNumber)
                .map(FaceEnrolmentResponse::from)
                .orElseGet(() -> FaceEnrolmentResponse.notEnrolled(computerNumber));
    }

    /** Withdrawing consent deletes the biometric template. */
    @Transactional
    public FaceEnrolmentResponse remove(String computerNumber) {
        templateRepository.findById(computerNumber).ifPresent(templateRepository::delete);
        return FaceEnrolmentResponse.notEnrolled(computerNumber);
    }
}
