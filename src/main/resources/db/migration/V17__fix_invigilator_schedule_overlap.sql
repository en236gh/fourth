-- Fix double-booking: invigilator@unza.zm was assigned to both Main LT 1 and
-- Natural Science Hall for CS101 at the same date/time (09:00–11:00).
-- Keep invigilator@unza.zm on Main LT 1 (demo students live there).
-- Move Natural Science Hall to jane.invigilator@unza.zm (no overlapping duties).

DELETE FROM public.invigilator_assignment ia
USING public.exam_session es, public.venue v, public.staff s
WHERE ia.exam_session_id = es.exam_session_id
  AND ia.venue_id = v.venue_id
  AND ia.staff_id = s.staff_id
  AND s.email = 'invigilator@unza.zm'
  AND es.course_code = 'CS101'
  AND es.exam_date = '2026-08-10'
  AND v.venue_name = 'Natural Science Hall';

INSERT INTO public.invigilator_assignment (exam_session_id, venue_id, staff_id)
SELECT es.exam_session_id, v.venue_id, s.staff_id
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Natural Science Hall'
JOIN public.staff s ON s.email = 'jane.invigilator@unza.zm'
JOIN public.exam_venue ev
  ON ev.exam_session_id = es.exam_session_id
 AND ev.venue_id = v.venue_id
WHERE es.course_code = 'CS101'
  AND es.exam_date = '2026-08-10'
ON CONFLICT DO NOTHING;
