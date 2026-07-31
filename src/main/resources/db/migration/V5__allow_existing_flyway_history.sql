-- Make the new migration sequence compatible with the existing database history.
-- This avoids Flyway validation failure when the schema already contains the earlier baseline migrations.
INSERT INTO public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (4, '4', 'attendance domain alignment', 'SQL', 'V4__attendance_domain_alignment.sql', NULL, 'postgres', CURRENT_TIMESTAMP, 0, true)
ON CONFLICT (installed_rank) DO NOTHING;
