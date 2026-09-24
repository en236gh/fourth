package com.backend.fourth.student.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StudentProgrammeEnrolmentRepository {
    private final JdbcTemplate jdbc;

    public Optional<ProgrammeEnrolment> findActiveForAcademicYear(
            String computerNumber, String academicYear) {
        return find("""
                WHERE e.computer_number = ?
                  AND e.academic_year = ?
                """, computerNumber, academicYear);
    }

    public Optional<ProgrammeEnrolment> findLatestActive(String computerNumber) {
        return find("""
                WHERE e.computer_number = ?
                """, computerNumber);
    }

    private Optional<ProgrammeEnrolment> find(String filter, String... parameters) {
        List<ProgrammeEnrolment> results = jdbc.query("""
                SELECT e.programme_id, p.programme_code, p.programme_name,
                       e.year_of_study, e.enrolment_status
                FROM public.student_programme_enrolment e
                JOIN public.programme p ON p.programme_id = e.programme_id
                """ + filter + """
                  AND e.enrolment_status IN ('ACTIVE', 'DEFERRED')
                  AND p.is_active
                ORDER BY e.updated_at DESC, e.enrolment_id DESC
                LIMIT 1
                """, (rs, rowNum) -> new ProgrammeEnrolment(
                rs.getInt("programme_id"),
                rs.getString("programme_code"),
                rs.getString("programme_name"),
                rs.getInt("year_of_study"),
                rs.getString("enrolment_status")
        ), (Object[]) parameters);
        return results.stream().findFirst();
    }

    public record ProgrammeEnrolment(
            Integer programmeId,
            String programmeCode,
            String programmeName,
            Integer yearOfStudy,
            String enrolmentStatus
    ) {
    }
}