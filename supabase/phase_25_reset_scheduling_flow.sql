-- DEVELOPMENT / TEST DATABASE ONLY: destructive, database-wide scheduling reset.
-- Run manually in the Supabase SQL Editor as the database owner. Stop the backend
-- first so background jobs and open dashboards cannot recreate activity mid-reset.
-- This is a manual utility, deliberately outside the Flyway migration directory.
--
-- Clears ALL admin venue bookings, student allocations, invigilator assignments,
-- exam sessions (including legacy sessions), periods and their course/slot setup,
-- amendments, notifications, requests, scheduling audit and exam activity/pass history.
-- Keeps accounts/passwords/roles, login tokens, students, academic catalogue,
-- student registrations/enrolments, lecturer-course links, venues/capacities,
-- venue unavailability and scheduling permissions.
-- Afterward, create a new examination period and select its courses and slots,
-- then generate/place exams, allocate students, assign invigilators and publish.
-- Existing venue unavailability still applies to the new schedule.
--
-- Requires scheduling migration V32 or later; later optional tables are included
-- when present. No CASCADE: an unexpected dependency aborts the entire reset.
-- Identity sequences are preserved so old exam/pass links do not reuse new IDs.

BEGIN;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '60s';
SELECT pg_advisory_xact_lock(741902, 1);

DO $$
DECLARE
    target_table text;
    qualified_tables text;
    remaining bigint;
    reset_tables constant text[] := ARRAY[
        'attendance',
        'incident',
        'generated_report',
        'examination_slip',
        'examination_pass',
        'examination_notification',
        'examination_amendment_allocation',
        'examination_amendment_duty',
        'examination_amendment_venue',
        'examination_amendment',
        'examination_change_request',
        'invigilator_assignment',
        'student_venue_allocation',
        'exam_venue',
        'exam_session_programme_course',
        'exam_session',
        'examination_period_course',
        'examination_period_slot',
        'examination_period',
        'scheduling_audit'
    ];
BEGIN
    IF to_regclass('public.examination_period') IS NULL THEN
        RAISE EXCEPTION 'Scheduling schema missing: apply V32 and subsequent migrations first';
    END IF;

    FOREACH target_table IN ARRAY reset_tables LOOP
        IF to_regclass(format('public.%I', target_table)) IS NOT NULL THEN
            qualified_tables := concat_ws(', ', qualified_tables,
                                          format('public.%I', target_table));
        END IF;
    END LOOP;

    -- One explicit TRUNCATE includes all known FK dependants together. PostgreSQL
    -- does not invoke row DELETE triggers, so published-schedule protections do
    -- not block this development reset. No triggers or constraints are disabled.
    EXECUTE 'TRUNCATE TABLE ' || qualified_tables || ' CONTINUE IDENTITY RESTRICT';

    FOREACH target_table IN ARRAY reset_tables LOOP
        IF to_regclass(format('public.%I', target_table)) IS NOT NULL THEN
            EXECUTE format('SELECT count(*) FROM public.%I', target_table) INTO remaining;
            IF remaining <> 0 THEN
                RAISE EXCEPTION 'Reset failed: % still contains % rows', target_table, remaining;
            END IF;
            RAISE NOTICE '%: % rows remaining', target_table, remaining;
        END IF;
    END LOOP;
END $$;

COMMIT;

SELECT 'Scheduling reset complete. Create a new examination period to begin.' AS result;
