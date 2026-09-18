-- Phase 11: Seed demo students.
-- Shared demo password for every row: e1n2o3c4h5
-- The value below is a BCrypt hash of that password.

BEGIN;

INSERT INTO public.student (
    computer_number, national_id, full_name, program, school, year_of_study,
    email, phone, photo_path, qr_token, status, password_hash,
    account_activated, activated_at
)
VALUES
    ('2022004264', '1234567890', 'K. Banda', 'Computer Science', 'School of Natural Sciences', 3, 'k.banda@dummy.unza', '+260971100001', '/img/students/kbanda.jpg', 'QR-CS-001', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004265', '1234567891', 'T. Mwewa', 'Computer Science', 'School of Natural Sciences', 3, 't.mwewa@dummy.unza', '+260971100002', '/img/students/tmwewa.jpg', 'QR-CS-002', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004266', '1234567892', 'L. Phiri', 'Computer Science', 'School of Natural Sciences', 3, 'l.phiri@dummy.unza', '+260971100003', '/img/students/lphiri.jpg', 'QR-CS-003', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004267', '1234567893', 'J. Sialumba', 'Electrical Engineering', 'School of Engineering', 4, 'j.sialumba@dummy.unza', '+260971100004', '/img/students/jsialumba.jpg', 'QR-EE-001', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004268', '1234567894', 'M. Mulenga', 'Electrical Engineering', 'School of Engineering', 4, 'm.mulenga@dummy.unza', '+260971100005', '/img/students/mmulenga.jpg', 'QR-EE-002', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004269', '1234567895', 'N. Chanda', 'Education', 'School of Education', 2, 'n.chanda@dummy.unza', '+260971100006', '/img/students/nchanda.jpg', 'QR-ED-001', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004270', '1234567896', 'P. Mwaba', 'Education', 'School of Education', 2, 'p.mwaba@dummy.unza', '+260971100007', '/img/students/pmwaba.jpg', 'QR-ED-002', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004271', '1234567897', 'S. Kalima', 'Business Administration', 'Graduate School of Business', 3, 's.kalima@dummy.unza', '+260971100008', '/img/students/skalima.jpg', 'QR-BA-001', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004272', '1234567898', 'A. Zulu', 'Computer Science', 'School of Natural Sciences', 2, 'a.zulu@dummy.unza', '+260971100009', '/img/students/azulu.jpg', 'QR-CS-004', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004273', '1234567899', 'B. Daka', 'Computer Science', 'School of Natural Sciences', 4, 'b.daka@dummy.unza', '+260971100010', '/img/students/bdaka.jpg', 'QR-CS-005', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004274', '1234567900', 'C. Bwalya', 'Electrical Engineering', 'School of Engineering', 3, 'c.bwalya@dummy.unza', '+260971100011', '/img/students/cbwalya.jpg', 'QR-EE-003', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004275', '1234567901', 'D. Lungu', 'Electrical Engineering', 'School of Engineering', 2, 'd.lungu@dummy.unza', '+260971100012', '/img/students/dlungu.jpg', 'QR-EE-004', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004276', '1234567902', 'E. Sakala', 'Education', 'School of Education', 3, 'e.sakala@dummy.unza', '+260971100013', '/img/students/esakala.jpg', 'QR-ED-003', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004277', '1234567903', 'F. Ngoma', 'Education', 'School of Education', 1, 'f.ngoma@dummy.unza', '+260971100014', '/img/students/fngoma.jpg', 'QR-ED-004', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004278', '1234567904', 'G. Tembo', 'Business Administration', 'Graduate School of Business', 2, 'g.tembo@dummy.unza', '+260971100015', '/img/students/gtembo.jpg', 'QR-BA-002', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004279', '1234567905', 'H. Phiri', 'Business Administration', 'Graduate School of Business', 4, 'h.phiri@dummy.unza', '+260971100016', '/img/students/hphiri.jpg', 'QR-BA-003', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004280', '1234567906', 'I. Mwale', 'Computer Science', 'School of Natural Sciences', 1, 'i.mwale@dummy.unza', '+260971100017', '/img/students/imwale.jpg', 'QR-CS-006', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004281', '1234567907', 'J. Chisenga', 'Computer Science', 'School of Natural Sciences', 3, 'j.chisenga@dummy.unza', '+260971100018', '/img/students/jchisenga.jpg', 'QR-CS-007', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004282', '1234567908', 'K. Musonda', 'Electrical Engineering', 'School of Engineering', 1, 'k.musonda@dummy.unza', '+260971100019', '/img/students/kmusonda.jpg', 'QR-EE-005', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP),
    ('2022004283', '1234567909', 'L. Chirwa', 'Education', 'School of Education', 4, 'l.chirwa@dummy.unza', '+260971100020', '/img/students/lchirwa.jpg', 'QR-ED-005', 'ACTIVE', '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3Kj3D3RMuQ1Ultk0dlmbFlZcY.', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (computer_number) DO UPDATE
SET national_id = EXCLUDED.national_id,
    full_name = EXCLUDED.full_name,
    program = EXCLUDED.program,
    school = EXCLUDED.school,
    year_of_study = EXCLUDED.year_of_study,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    photo_path = EXCLUDED.photo_path,
    qr_token = EXCLUDED.qr_token,
    status = EXCLUDED.status,
    password_hash = EXCLUDED.password_hash,
    account_activated = EXCLUDED.account_activated,
    activated_at = EXCLUDED.activated_at;

COMMIT;