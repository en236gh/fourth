-- Venue assignments remain; students no longer have numbered seats.
ALTER TABLE public.student_venue_allocation DROP COLUMN IF EXISTS seat_number;
