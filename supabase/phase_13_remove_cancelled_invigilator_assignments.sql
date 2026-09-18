-- Phase 13: Remove assignment rows that were previously cancelled.
--
-- Run the SELECT first in Supabase to review the rows. The DELETE removes
-- cancelled rows so they no longer appear as assigned in admin staffing views.

BEGIN;

SELECT exam_session_id, venue_id, staff_id, assignment_status
FROM public.invigilator_assignment
WHERE assignment_status = 'CANCELLED';

DELETE FROM public.invigilator_assignment
WHERE assignment_status = 'CANCELLED';

COMMIT;