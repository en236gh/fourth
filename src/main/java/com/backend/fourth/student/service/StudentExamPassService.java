package com.backend.fourth.student.service;

import com.backend.fourth.allocation.entity.StudentVenueAllocation;
import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.student.dto.ExaminationPassExamItem;
import com.backend.fourth.student.dto.ExaminationPassResponse;
import com.backend.fourth.student.dto.StudentExaminationSummaryResponse;
import com.backend.fourth.student.entity.ExaminationPass;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.entity.StudentRegistration;
import com.backend.fourth.student.repository.ExaminationPassRepository;
import com.backend.fourth.student.repository.StudentRegistrationRepository;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentExamPassService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StudentRegistrationRepository studentRegistrationRepository;
    private final ExamSessionRepository examSessionRepository;
    private final StudentVenueAllocationRepository allocationRepository;
    private final VenueRepository venueRepository;
    private final ExaminationPassRepository examinationPassRepository;
    private final ExamPassQrService examPassQrService;
    private final ExamPassPdfService examPassPdfService;

    @Transactional(readOnly = true)
    public List<StudentExaminationSummaryResponse> listMyExaminations(Student student) {
        List<StudentRegistration> registrations =
                studentRegistrationRepository.findByComputerNumber(student.getComputerNumber());

        Map<String, ExaminationPass> passesByPeriod = examinationPassRepository
                .findByStudentComputerNumber(student.getComputerNumber())
                .stream()
                .collect(Collectors.toMap(
                        pass -> periodKey(pass.getAcademicYear(), pass.getSemester()),
                        Function.identity(),
                        (first, ignored) -> first));

        List<StudentExaminationSummaryResponse> results = new ArrayList<>();
        for (StudentRegistration registration : registrations) {
            List<ExamSession> sessions = examSessionRepository.findByCourseCodeAndAcademicYearAndSemester(
                    registration.getCourseCode(),
                    registration.getAcademicYear(),
                    registration.getSemester());

            ExaminationPass pass = passesByPeriod.get(
                    periodKey(registration.getAcademicYear(), registration.getSemester()));

            for (ExamSession session : sessions) {
                Optional<StudentVenueAllocation> allocation = allocationRepository
                        .findByComputerNumberAndExamSessionId(
                                student.getComputerNumber(), session.getExamSessionId());

                Venue venue = allocation
                        .flatMap(a -> venueRepository.findById(a.getVenueId()))
                        .orElse(null);

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
                        pass != null,
                        pass != null ? pass.getPassId() : null
                ));
            }
        }

        results.sort(Comparator
                .comparing(StudentExaminationSummaryResponse::examDate)
                .thenComparing(StudentExaminationSummaryResponse::startTime));
        return results;
    }

    @Transactional
    public ExaminationPassResponse generatePass(Student student, String academicYear, Integer semester) {
        PeriodContext context = requireEligiblePeriod(student, academicYear, semester);

        ExamPassQrService.SignedQrToken signed = examPassQrService.sign(
                student.getComputerNumber(),
                context.academicYear(),
                context.semester(),
                context.allocatedSessions());

        ExaminationPass pass = examinationPassRepository
                .findByStudentComputerNumberAndAcademicYearAndSemester(
                        student.getComputerNumber(), context.academicYear(), context.semester())
                .orElseGet(ExaminationPass::new);

        pass.setStudent(student);
        pass.setAcademicYear(context.academicYear());
        pass.setSemester(context.semester());
        pass.setQrToken(signed.token());
        pass.setQrJti(signed.jti());
        pass.setGeneratedAt(LocalDateTime.now());
        pass.setExpiresAt(LocalDateTime.ofInstant(signed.expiresAt(), ZoneId.systemDefault()));
        examinationPassRepository.save(pass);

        return toPassResponse(student, context, pass);
    }

    @Transactional(readOnly = true)
    public ExaminationPassResponse getPass(Student student, String academicYear, Integer semester) {
        PeriodContext context = requireEligiblePeriod(student, academicYear, semester);
        ExaminationPass pass = examinationPassRepository
                .findByStudentComputerNumberAndAcademicYearAndSemester(
                        student.getComputerNumber(), context.academicYear(), context.semester())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Examination pass has not been generated yet. Generate it first."));
        return toPassResponse(student, context, pass);
    }

    @Transactional(readOnly = true)
    public byte[] downloadPassPdf(Student student, String academicYear, Integer semester) {
        PeriodContext context = requireEligiblePeriod(student, academicYear, semester);
        ExaminationPass pass = examinationPassRepository
                .findByStudentComputerNumberAndAcademicYearAndSemester(
                        student.getComputerNumber(), context.academicYear(), context.semester())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Examination pass has not been generated yet. Generate it first."));

        List<ExamPassPdfService.ExaminationRow> rows = context.examItems().stream()
                .map(item -> new ExamPassPdfService.ExaminationRow(
                        item.courseCode(),
                        item.examDate(),
                        item.startTime(),
                        item.endTime(),
                        item.venueName(),
                        item.building(),
                        item.seatNumber()))
                .toList();

        return examPassPdfService.buildPdf(new ExamPassPdfService.ExaminationPassDocumentData(
                student.getComputerNumber(),
                student.getFullName(),
                student.getSchool(),
                student.getProgram(),
                student.getYearOfStudy(),
                context.academicYear(),
                context.semester(),
                rows,
                pass.getQrToken(),
                pass.getGeneratedAt().format(TIMESTAMP),
                pass.getExpiresAt().format(TIMESTAMP)
        ));
    }

    private PeriodContext requireEligiblePeriod(Student student, String academicYear, Integer semester) {
        if (!student.isAccountActivated()) {
            throw new IllegalStateException("Student account is not activated");
        }

        List<StudentRegistration> registrations =
                studentRegistrationRepository.findByComputerNumber(student.getComputerNumber());
        if (registrations.isEmpty()) {
            throw new IllegalArgumentException("You have no course registrations");
        }

        PeriodKey period = resolvePeriod(registrations, academicYear, semester);

        List<StudentRegistration> periodRegistrations = registrations.stream()
                .filter(r -> Objects.equals(r.getAcademicYear(), period.academicYear())
                        && Objects.equals(r.getSemester(), period.semester()))
                .toList();

        List<AllocatedExam> allocated = new ArrayList<>();
        for (StudentRegistration registration : periodRegistrations) {
            List<ExamSession> sessions = examSessionRepository.findByCourseCodeAndAcademicYearAndSemester(
                    registration.getCourseCode(),
                    registration.getAcademicYear(),
                    registration.getSemester());

            for (ExamSession session : sessions) {
                Optional<StudentVenueAllocation> allocation = allocationRepository
                        .findByComputerNumberAndExamSessionId(
                                student.getComputerNumber(), session.getExamSessionId());
                if (allocation.isEmpty()) {
                    continue;
                }

                Venue venue = venueRepository.findById(allocation.get().getVenueId())
                        .orElseThrow(() -> new IllegalStateException("Allocated venue not found"));

                allocated.add(new AllocatedExam(session, allocation.get(), venue));
            }
        }

        if (allocated.isEmpty()) {
            throw new IllegalStateException(
                    "Venue allocation is required for at least one examination before generating a pass");
        }

        allocated.sort(Comparator
                .comparing((AllocatedExam a) -> a.session().getExamDate())
                .thenComparing(a -> a.session().getStartTime()));

        List<ExaminationPassExamItem> items = allocated.stream()
                .map(a -> new ExaminationPassExamItem(
                        a.session().getExamSessionId(),
                        a.session().getCourseCode(),
                        a.session().getExamDate().format(DATE),
                        a.session().getStartTime().format(TIME),
                        a.session().getEndTime().format(TIME),
                        a.session().getExamType(),
                        a.session().getStatus(),
                        a.venue().getVenueName(),
                        a.venue().getBuilding(),
                        a.allocation().getSeatNumber()))
                .toList();

        List<ExamSession> sessions = allocated.stream().map(AllocatedExam::session).toList();
        return new PeriodContext(period.academicYear(), period.semester(), sessions, items);
    }

    private PeriodKey resolvePeriod(
            List<StudentRegistration> registrations, String academicYear, Integer semester) {
        Map<String, PeriodKey> periods = new LinkedHashMap<>();
        for (StudentRegistration registration : registrations) {
            periods.putIfAbsent(
                    periodKey(registration.getAcademicYear(), registration.getSemester()),
                    new PeriodKey(registration.getAcademicYear(), registration.getSemester()));
        }

        if (academicYear != null && !academicYear.isBlank() && semester != null) {
            PeriodKey requested = new PeriodKey(academicYear.trim(), semester);
            if (!periods.containsKey(periodKey(requested.academicYear(), requested.semester()))) {
                throw new IllegalArgumentException(
                        "You have no registrations for " + requested.academicYear()
                                + " semester " + requested.semester());
            }
            return requested;
        }

        if (periods.size() == 1) {
            return periods.values().iterator().next();
        }

        throw new IllegalArgumentException(
                "Multiple exam periods found. Provide academicYear and semester query parameters.");
    }

    private ExaminationPassResponse toPassResponse(
            Student student, PeriodContext context, ExaminationPass pass) {
        return new ExaminationPassResponse(
                pass.getPassId(),
                context.academicYear(),
                context.semester(),
                student.getComputerNumber(),
                student.getFullName(),
                student.getSchool(),
                student.getProgram(),
                student.getYearOfStudy(),
                pass.getQrToken(),
                examPassPdfService.toQrImageBase64(pass.getQrToken()),
                pass.getGeneratedAt().format(TIMESTAMP),
                pass.getExpiresAt().format(TIMESTAMP),
                context.examItems()
        );
    }

    private static String periodKey(String academicYear, Integer semester) {
        return academicYear + "|" + semester;
    }

    private record PeriodKey(String academicYear, Integer semester) {
    }

    private record AllocatedExam(
            ExamSession session,
            StudentVenueAllocation allocation,
            Venue venue
    ) {
    }

    private record PeriodContext(
            String academicYear,
            Integer semester,
            List<ExamSession> allocatedSessions,
            List<ExaminationPassExamItem> examItems
    ) {
    }
}
