-- Snapshots retain membership, not copies of student photos or examination-pass tokens.
CREATE TABLE attendance_offline_snapshot (
    snapshot_id UUID PRIMARY KEY,
    staff_id INTEGER NOT NULL REFERENCES staff(staff_id),
    generated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE attendance_offline_roster (
    snapshot_id UUID NOT NULL REFERENCES attendance_offline_snapshot(snapshot_id) ON DELETE CASCADE,
    exam_session_id INTEGER NOT NULL,
    venue_id INTEGER NOT NULL,
    computer_number VARCHAR(20) NOT NULL,
    PRIMARY KEY (snapshot_id, exam_session_id, venue_id, computer_number)
);
CREATE INDEX attendance_offline_snapshot_staff ON attendance_offline_snapshot(staff_id);

-- UUID is global; ownership is checked before exposing a previous outcome.
-- Rejected outcomes are retained as well as successful ones.
CREATE TABLE attendance_sync_scan (
    scan_id UUID PRIMARY KEY,
    staff_id INTEGER NOT NULL REFERENCES staff(staff_id),
    outcome VARCHAR(30) NOT NULL CHECK (outcome IN ('ACCEPTED','ALREADY_RECORDED','REJECTED')),
    reason VARCHAR(80) NOT NULL,
    message TEXT NOT NULL,
    attendance_id INTEGER REFERENCES attendance(attendance_id),
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX attendance_sync_scan_staff ON attendance_sync_scan(staff_id);
ALTER TABLE attendance ADD COLUMN client_scan_id UUID UNIQUE REFERENCES attendance_sync_scan(scan_id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE attendance ADD COLUMN processed_at TIMESTAMPTZ;
-- Historical processing times are unknown; leave historical rows NULL.
ALTER TABLE attendance ALTER COLUMN processed_at SET DEFAULT CURRENT_TIMESTAMP;
