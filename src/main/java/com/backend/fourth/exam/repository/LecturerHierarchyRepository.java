package com.backend.fourth.exam.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class LecturerHierarchyRepository {
    private final JdbcTemplate jdbc;

    public List<Map<String, Object>> findByStaffId(Integer staffId) {
        return jdbc.queryForList("""
                SELECT s.school_id, s.school_name, p.programme_id, p.programme_code, p.programme_name,
                       m.major_id, m.major_code, m.major_name, pc.programme_course_id,
                       pc.year_of_study, pc.semester, c.course_code, c.course_name
                FROM public.course_lecturer cl
                JOIN public.course c ON c.course_code = cl.course_code
                JOIN public.programme_course pc ON pc.course_code = c.course_code
                JOIN public.programme p ON p.programme_id = pc.programme_id
                JOIN public.school s ON s.school_id = p.school_id
                LEFT JOIN public.major m ON m.major_id = pc.major_id AND m.programme_id = p.programme_id
                WHERE cl.staff_id = ? AND s.is_active AND p.is_active AND c.is_active AND pc.is_active
                  AND (pc.major_id IS NULL OR m.is_active)
                ORDER BY s.school_name, p.programme_name, m.major_name, pc.year_of_study, pc.semester, c.course_code
                """, staffId);
    }
}
