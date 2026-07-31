-- Ensure a clean, error-free invigilator demo path for Postman / frontend testing.
-- Target account: invigilator@unza.zm
-- Target exam: CS101 (exam_session for 2026-08-10)
-- Target venue: Main LT 1 (matches existing allocations)

-- Guarantee CS101 student 2022004266 is allocated to Main LT 1 (same venue as invigilator).
INSERT INTO public.student_venue_allocation (computer_number, exam_session_id, venue_id, seat_number)
SELECT '2022004266', es.exam_session_id, v.venue_id, 'A03'
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Main LT 1'
WHERE es.course_code = 'CS101'
  AND es.exam_date = '2026-08-10'
  AND NOT EXISTS (
      SELECT 1
      FROM public.student_venue_allocation a
      WHERE a.computer_number = '2022004266'
        AND a.exam_session_id = es.exam_session_id
  );

-- Guarantee invigilator assignment to Main LT 1 for that exam (idempotent).
INSERT INTO public.invigilator_assignment (exam_session_id, venue_id, staff_id)
SELECT es.exam_session_id, v.venue_id, s.staff_id
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Main LT 1'
JOIN public.staff s ON s.email = 'invigilator@unza.zm'
JOIN public.exam_venue ev
  ON ev.exam_session_id = es.exam_session_id
 AND ev.venue_id = v.venue_id
WHERE es.course_code = 'CS101'
  AND es.exam_date = '2026-08-10'
ON CONFLICT DO NOTHING;

-- Ensure exam_venue link exists for Main LT 1 / CS101.
INSERT INTO public.exam_venue (exam_session_id, venue_id)
SELECT es.exam_session_id, v.venue_id
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Main LT 1'
WHERE es.course_code = 'CS101'
  AND es.exam_date = '2026-08-10'
ON CONFLICT DO NOTHING;
