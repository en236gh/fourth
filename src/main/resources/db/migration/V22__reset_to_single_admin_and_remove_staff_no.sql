-- Start with an empty operational database. The application initializer recreates
-- the single administrator after this migration completes.
TRUNCATE TABLE
    public.attendance,
    public.course_lecturer,
    public.exam_venue,
    public.examination_pass,
    public.generated_report,
    public.incident,
    public.invigilator_assignment,
    public.refresh_token,
    public.staff_role,
    public.student_refresh_token,
    public.student_registration,
    public.student_venue_allocation,
    public.exam_session,
    public.staff,
    public.student,
    public.venue
RESTART IDENTITY CASCADE;

ALTER TABLE public.staff
    DROP COLUMN IF EXISTS staff_no;
