package com.backend.fourth.student.service;

import com.backend.fourth.allocation.entity.StudentVenueAllocation;
import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.student.dto.ExaminationSlipResponse;
import com.backend.fourth.student.dto.StudentExaminationSummaryResponse;
import com.backend.fourth.student.entity.ExaminationSlip;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.entity.StudentRegistration;
import com.backend.fourth.student.repository.ExaminationSlipRepository;
import com.backend.fourth.student.repository.StudentRegistrationRepository;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentExamSlipService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StudentRegistrationRepository studentRegistrationRepository;
    private final ExamSessionRepository examSessionRepository;
    private final StudentVenueAllocationRepository allocationRepository;
    private final VenueRepository venueRepository;
    private final ExaminationSlipRepository examinationSlipRepository;
    private final ExamSlipQrService examSlipQrService;
    private final ExamSlipPdfService examSlipPdfService;

    @Transactional(readOnly = true)
    public List<StudentExaminationSummaryResponse> listMyExaminations(Student student) {
        List<StudentRegistration> registrations =
                studentRegistrationRepository.findByComputerNumber(student.getComputerNumber());

        Map<Integer, ExaminationSlip> slipsByExam = examinationSlipRepository
                .findByStudentComputerNumberOrderByGeneratedAtDesc(student.getComputerNumber())
                .stream()
                .collect(Collectors.toMap(
                        ExaminationSlip::getExamSessionId,
                        Function.identity(),
                        (first, ignored) -> first));

        List<StudentExaminationSummaryResponse> results = new ArrayList<>();
        for (StudentRegistration registration : registrations) {
            List<ExamSession> sessions = examSessionRepository.findByCourseCodeAndAcademicYearAndSemester(
                    registration.getCourseCode(),
                    registration.getAcademicYear(),
                    registration.getSemester());

            for (ExamSession session : sessions) {
                Optional<StudentVenueAllocation> allocation = allocationRepository
                        .findByComputerNumberAndExamSessionId(
                                student.getComputerNumber(), session.getExamSessionId());

                Venue venue = allocation
                        .flatMap(a -> venueRepository.findById(a.getVenueId()))
                        .orElse(null);

                ExaminationSlip slip = slipsByExam.get(session.getExamSessionId());

                results.add(new StudentExaminationSummaryResponse(
                        session.getExamSessionId(),
                        session.getCourseCode(),
                        session.getExamDate().format(DATE),
                        session.getStartTime().format(TIME),
                        session.getEndTime().format(TIME),
                        session.getAcademicYear(),
                        session.getSemester(),
                        session.getExamType(),
                        session.getStatus(),
                        allocation.isPresent(),
                        venue != null ? venue.getVenueName() : null,
                        venue != null ? venue.getBuilding() : null,
                        allocation.map(StudentVenueAllocation::getSeatNumber).orElse(null),
                        slip != null,
                        slip != null ? slip.getSlipId() : null
                ));
            }
        }

        results.sort(Comparator
                .comparing(StudentExaminationSummaryResponse::examDate)
                .thenComparing(StudentExaminationSummaryResponse::startTime));
        return results;
    }

    @Transactional
    public ExaminationSlipResponse generateSlip(Student student, Integer examSessionId) {
        ExamContext context = requireEligibleExam(student, examSessionId);

        ExamSlipQrService.SignedQrToken signed = examSlipQrService.sign(
                student.getComputerNumber(), context.examSession());

        ExaminationSlip slip = examinationSlipRepository
                .findByStudentComputerNumberAndExamSessionId(student.getComputerNumber(), examSessionId)
                .orElseGet(ExaminationSlip::new);

        slip.setStudent(student);
        slip.setExamSessionId(examSessionId);
        slip.setQrToken(signed.token());
        slip.setQrJti(signed.jti());
        slip.setGeneratedAt(LocalDateTime.now());
        examinationSlipRepository.save(slip);

        return toSlipResponse(student, context, slip);
    }

    @Transactional(readOnly = true)
    public ExaminationSlipResponse getSlip(Student student, Integer examSessionId) {
        ExamContext context = requireEligibleExam(student, examSessionId);
        ExaminationSlip slip = examinationSlipRepository
                .findByStudentComputerNumberAndExamSessionId(student.getComputerNumber(), examSessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Examination slip has not been generated yet. Generate it first."));
        return toSlipResponse(student, context, slip);
    }

    @Transactional(readOnly = true)
    public byte[] downloadSlipPdf(Student student, Integer examSessionId) {
        ExamContext context = requireEligibleExam(student, examSessionId);
        ExaminationSlip slip = examinationSlipRepository
                .findByStudentComputerNumberAndExamSessionId(student.getComputerNumber(), examSessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Examination slip has not been generated yet. Generate it first."));

        return examSlipPdfService.buildPdf(new ExamSlipPdfService.ExaminationSlipDocumentData(
                student.getComputerNumber(),
                student.getFullName(),
                student.getSchool(),
                student.getProgram(),
                student.getYearOfStudy(),
                context.examSession().getCourseCode(),
                context.examSession().getExamType(),
                context.examSession().getAcademicYear(),
                context.examSession().getSemester(),
                context.examSession().getExamDate().format(DATE),
                context.examSession().getStartTime().format(TIME),
                context.examSession().getEndTime().format(TIME),
                context.examSession().getStatus(),
                context.venue().getVenueName(),
                context.venue().getBuilding(),
                context.allocation().getSeatNumber(),
                slip.getQrToken(),
                slip.getGeneratedAt().format(TIMESTAMP)
        ));
    }

    private ExamContext requireEligibleExam(Student student, Integer examSessionId) {
        if (!student.isAccountActivated()) {
            throw new IllegalStateException("Student account is not activated");
        }

        ExamSession examSession = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));

        boolean registered = studentRegistrationRepository
                .existsByComputerNumberAndCourseCodeAndAcademicYearAndSemester(
                        student.getComputerNumber(),
                        examSession.getCourseCode(),
                        examSession.getAcademicYear(),
                        examSession.getSemester());
        if (!registered) {
            throw new IllegalArgumentException("You are not registered for this examination");
        }

        StudentVenueAllocation allocation = allocationRepository
                .findByComputerNumberAndExamSessionId(student.getComputerNumber(), examSessionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Venue allocation is required before generating an examination slip"));

        Venue venue = venueRepository.findById(allocation.getVenueId())
                .orElseThrow(() -> new IllegalStateException("Allocated venue not found"));

        return new ExamContext(examSession, allocation, venue);
    }

    private ExaminationSlipResponse toSlipResponse(Student student, ExamContext context, ExaminationSlip slip) {
        return new ExaminationSlipResponse(
                slip.getSlipId(),
                context.examSession().getExamSessionId(),
                context.examSession().getCourseCode(),
                context.examSession().getExamDate().format(DATE),
                context.examSession().getStartTime().format(TIME),
                context.examSession().getEndTime().format(TIME),
                context.examSession().getAcademicYear(),
                context.examSession().getSemester(),
                context.examSession().getExamType(),
                context.examSession().getStatus(),
                student.getComputerNumber(),
                student.getFullName(),
                student.getSchool(),
                student.getProgram(),
                student.getYearOfStudy(),
                context.venue().getVenueName(),
                context.venue().getBuilding(),
                context.allocation().getSeatNumber(),
                slip.getQrToken(),
                examSlipPdfService.toQrImageBase64(slip.getQrToken()),
                slip.getGeneratedAt().format(TIMESTAMP)
        );
    }

    private record ExamContext(
            ExamSession examSession,
            StudentVenueAllocation allocation,
            Venue venue
    ) {
    }
}
