package com.backend.fourth.invigilator.repository;

import com.backend.fourth.invigilator.dto.AcademicSelection;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AssignmentAcademicRepository {
    private final JdbcTemplate jdbc;

    private static final String CURRICULUM = """
            FROM public.programme_course pc
            JOIN public.programme p ON p.programme_id = pc.programme_id
            JOIN public.school s ON s.school_id = p.school_id
            JOIN public.course c ON c.course_code = pc.course_code
            """;
    private static final String ACTIVE = " s.is_active AND p.is_active AND c.is_active ";
    private static final String EXAMS = CURRICULUM + """
            JOIN public.exam_session_programme_course epc ON epc.programme_course_id = pc.programme_course_id
            JOIN public.exam_session es ON es.exam_session_id = epc.exam_session_id
                AND es.course_code = pc.course_code AND es.semester = pc.semester
            """;

    public List<Map<String, Object>> schools() {
        return jdbc.queryForList("SELECT school_id, school_name FROM public.school WHERE is_active ORDER BY school_name");
    }

    public List<Map<String, Object>> programmes(int schoolId) {
        return jdbc.queryForList("""
                SELECT p.programme_id, p.programme_name FROM public.programme p
                JOIN public.school s ON s.school_id = p.school_id
                WHERE s.school_id = ? AND s.is_active AND p.is_active ORDER BY p.programme_name
                """, schoolId);
    }

    public List<Map<String, Object>> years(int schoolId, int programmeId) {
        return jdbc.queryForList("SELECT DISTINCT pc.year_of_study " + CURRICULUM
                + " WHERE " + ACTIVE + " AND s.school_id = ? AND p.programme_id = ? ORDER BY pc.year_of_study",
                schoolId, programmeId);
    }

    public List<Map<String, Object>> courses(int schoolId, int programmeId, int yearOfStudy) {
        return jdbc.queryForList("SELECT DISTINCT c.course_code, c.course_name, pc.semester " + CURRICULUM
                + " WHERE " + ACTIVE + " AND s.school_id = ? AND p.programme_id = ? AND pc.year_of_study = ?"
                + " ORDER BY pc.semester, c.course_code", schoolId, programmeId, yearOfStudy);
    }

    public List<Map<String, Object>> exams(AcademicSelection selection) {
        return jdbc.queryForList("SELECT DISTINCT es.* " + EXAMS + " WHERE " + ACTIVE
                + " AND s.school_id = ? AND p.programme_id = ? AND pc.year_of_study = ? AND c.course_code = ?"
                + " AND es.status <> 'COMPLETED' ORDER BY es.exam_date, es.start_time, es.exam_session_id",
                selection.schoolId(), selection.programmeId(), selection.yearOfStudy(), selection.courseCode());
    }

    public boolean matches(Integer examSessionId, AcademicSelection selection) {
        if (selection == null) return false;
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 " + EXAMS + " WHERE " + ACTIVE
                + " AND s.school_id = ? AND p.programme_id = ? AND pc.year_of_study = ?"
                + " AND c.course_code = ? AND es.exam_session_id = ?)", Boolean.class,
                selection.schoolId(), selection.programmeId(), selection.yearOfStudy(), selection.courseCode(), examSessionId));
    }
}
