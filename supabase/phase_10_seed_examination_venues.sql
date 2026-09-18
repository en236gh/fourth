-- Phase 10: Seed UNZA examination venues from unza_venues.csv.
-- Capacity labels from the source CSV are mapped as follows:
-- Large = 500, Medium = 200, Varies = 300.

BEGIN;

INSERT INTO public.venue (venue_name, building, capacity)
SELECT v.venue_name, v.building, v.capacity
FROM (
    VALUES
        ('Michael J. Kelly Lecture Theatre (formerly NELT)', 'Great East Road Campus', 500),
        ('Teaching & Learning Complex LT 1', 'Great East Road Campus', 350),
        ('Teaching & Learning Complex LT 2', 'Great East Road Campus', 350),
        ('ACEIDAH Lecture Theatre', 'Great East Road Campus', 360),
        ('School of Engineering LT 1', 'Great East Road Campus', 250),
        ('School of Engineering LT 2', 'Great East Road Campus', 250),
        ('School of Public Health Lecture Theatre', 'Ridgeway Campus', 500),
        ('Special Needs Education Centre Auditorium', 'Great East Road Campus', 200),
        ('UNZA Main Library Auditorium', 'Great East Road Campus', 500),
        ('Confucius Institute Lecture Hall', 'Great East Road Campus', 200),
        ('Goma Lakes Open Space', 'Great East Road Campus', 300),
        ('Graduare Conference Facility 1', 'Great East Road Campus', 60),
        ('Graduare Conference Facility 2', 'Great East Road Campus', 60)
) AS v(venue_name, building, capacity)
WHERE NOT EXISTS (
    SELECT 1
    FROM public.venue existing
    WHERE existing.venue_name = v.venue_name
);

-- Assign each seeded examination session to a venue by course family.
-- The joins keep this seed independent of generated exam_session_id and venue_id values.
INSERT INTO public.exam_venue (exam_session_id, venue_id)
SELECT es.exam_session_id, v.venue_id
FROM public.exam_session es
JOIN (
    VALUES
        ('CSC', 'Michael J. Kelly Lecture Theatre (formerly NELT)'),
        ('MAT', 'Teaching & Learning Complex LT 1'),
        ('ENG', 'Teaching & Learning Complex LT 2'),
        ('EDU', 'Special Needs Education Centre Auditorium'),
        ('EEE', 'School of Engineering LT 1'),
        ('CEE', 'School of Engineering LT 2'),
        ('MEE', 'School of Public Health Lecture Theatre')
) AS assignment(course_prefix, venue_name)
    ON es.course_code LIKE assignment.course_prefix || '%'
JOIN public.venue v ON v.venue_name = assignment.venue_name
WHERE es.academic_year = '2026/2027'
  AND es.semester = 1
  AND es.exam_type = 'FINAL'
ON CONFLICT (exam_session_id, venue_id) DO NOTHING;

COMMIT;

-- Source notes are retained here because public.venue has no notes column:
-- Michael J. Kelly Lecture Theatre: Renamed in honor of Prof. Michael J. Kelly.
-- Teaching & Learning Complex LT 1/LT 2: Constructed via Graduare Property Development partnership.
-- ACEIDAH Lecture Theatre: African Centre of Excellence for Infectious Diseases.
-- School of Engineering LT 1/LT 2: Expanded infrastructure expansion.
-- School of Public Health Lecture Theatre: Supported by US Govt/CDC.
-- Special Needs Education Centre Auditorium: School of Education, supported by CBM Germany.
-- UNZA Main Library Auditorium: Central university library hub.
-- Confucius Institute Lecture Hall: Located near the Population Studies teaching building.
-- Goma Lakes Open Space: Often used for events, located near Student Affairs Unit.
-- Graduare Conference Facility 1/2: Part of the Teaching & Learning Complex.