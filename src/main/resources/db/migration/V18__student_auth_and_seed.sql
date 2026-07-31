-- Phase 1: student account activation fields, refresh tokens, STUDENT role,
-- school/faculty column, and expand seed students to ~20 (10-digit computer numbers).

ALTER TABLE public.student
    ADD COLUMN IF NOT EXISTS school VARCHAR(120),
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS account_activated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS activated_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE public.student
SET school = CASE program
    WHEN 'Computer Science' THEN 'School of Natural Sciences'
    WHEN 'Electrical Engineering' THEN 'School of Engineering'
    WHEN 'Education' THEN 'School of Education'
    WHEN 'Business Administration' THEN 'Graduate School of Business'
    ELSE 'School of Natural Sciences'
END
WHERE school IS NULL;

ALTER TABLE public.student
    ALTER COLUMN school SET NOT NULL;

INSERT INTO public.role (name)
VALUES ('STUDENT')
ON CONFLICT (name) DO NOTHING;

CREATE TABLE IF NOT EXISTS public.student_refresh_token (
    token_id BIGSERIAL PRIMARY KEY,
    computer_number VARCHAR(15) NOT NULL REFERENCES public.student(computer_number) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_student_refresh_token_student
    ON public.student_refresh_token(computer_number);

-- Ensure the original 8 demo students remain, then add more for a ~20-student set.
INSERT INTO public.student (
    computer_number, national_id, full_name, program, school, year_of_study,
    email, phone, photo_path, qr_token, status, account_activated
)
VALUES
    ('2022004264', '1234567890', 'K. Banda', 'Computer Science', 'School of Natural Sciences', 3,
     'k.banda@dummy.unza', '+260971100001', '/img/students/kbanda.jpg', 'QR-CS-001', 'ACTIVE', FALSE),
    ('2022004265', '1234567891', 'T. Mwewa', 'Computer Science', 'School of Natural Sciences', 3,
     't.mwewa@dummy.unza', '+260971100002', '/img/students/tmwewa.jpg', 'QR-CS-002', 'ACTIVE', FALSE),
    ('2022004266', '1234567892', 'L. Phiri', 'Computer Science', 'School of Natural Sciences', 3,
     'l.phiri@dummy.unza', '+260971100003', '/img/students/lphiri.jpg', 'QR-CS-003', 'ACTIVE', FALSE),
    ('2022004267', '1234567893', 'J. Sialumba', 'Electrical Engineering', 'School of Engineering', 4,
     'j.sialumba@dummy.unza', '+260971100004', '/img/students/jsialumba.jpg', 'QR-EE-001', 'ACTIVE', FALSE),
    ('2022004268', '1234567894', 'M. Mulenga', 'Electrical Engineering', 'School of Engineering', 4,
     'm.mulenga@dummy.unza', '+260971100005', '/img/students/mmulenga.jpg', 'QR-EE-002', 'ACTIVE', FALSE),
    ('2022004269', '1234567895', 'N. Chanda', 'Education', 'School of Education', 2,
     'n.chanda@dummy.unza', '+260971100006', '/img/students/nchanda.jpg', 'QR-ED-001', 'ACTIVE', FALSE),
    ('2022004270', '1234567896', 'P. Mwaba', 'Education', 'School of Education', 2,
     'p.mwaba@dummy.unza', '+260971100007', '/img/students/pmwaba.jpg', 'QR-ED-002', 'ACTIVE', FALSE),
    ('2022004271', '1234567897', 'S. Kalima', 'Business Administration', 'Graduate School of Business', 3,
     's.kalima@dummy.unza', '+260971100008', '/img/students/skalima.jpg', 'QR-BA-001', 'ACTIVE', FALSE),
    ('2022004272', '1234567898', 'A. Zulu', 'Computer Science', 'School of Natural Sciences', 2,
     'a.zulu@dummy.unza', '+260971100009', '/img/students/azulu.jpg', 'QR-CS-004', 'ACTIVE', FALSE),
    ('2022004273', '1234567899', 'B. Daka', 'Computer Science', 'School of Natural Sciences', 4,
     'b.daka@dummy.unza', '+260971100010', '/img/students/bdaka.jpg', 'QR-CS-005', 'ACTIVE', FALSE),
    ('2022004274', '1234567900', 'C. Bwalya', 'Electrical Engineering', 'School of Engineering', 3,
     'c.bwalya@dummy.unza', '+260971100011', '/img/students/cbwalya.jpg', 'QR-EE-003', 'ACTIVE', FALSE),
    ('2022004275', '1234567901', 'D. Lungu', 'Electrical Engineering', 'School of Engineering', 2,
     'd.lungu@dummy.unza', '+260971100012', '/img/students/dlungu.jpg', 'QR-EE-004', 'ACTIVE', FALSE),
    ('2022004276', '1234567902', 'E. Sakala', 'Education', 'School of Education', 3,
     'e.sakala@dummy.unza', '+260971100013', '/img/students/esakala.jpg', 'QR-ED-003', 'ACTIVE', FALSE),
    ('2022004277', '1234567903', 'F. Ngoma', 'Education', 'School of Education', 1,
     'f.ngoma@dummy.unza', '+260971100014', '/img/students/fngoma.jpg', 'QR-ED-004', 'ACTIVE', FALSE),
    ('2022004278', '1234567904', 'G. Tembo', 'Business Administration', 'Graduate School of Business', 2,
     'g.tembo@dummy.unza', '+260971100015', '/img/students/gtembo.jpg', 'QR-BA-002', 'ACTIVE', FALSE),
    ('2022004279', '1234567905', 'H. Phiri', 'Business Administration', 'Graduate School of Business', 4,
     'h.phiri@dummy.unza', '+260971100016', '/img/students/hphiri.jpg', 'QR-BA-003', 'ACTIVE', FALSE),
    ('2022004280', '1234567906', 'I. Mwale', 'Computer Science', 'School of Natural Sciences', 1,
     'i.mwale@dummy.unza', '+260971100017', '/img/students/imwale.jpg', 'QR-CS-006', 'ACTIVE', FALSE),
    ('2022004281', '1234567907', 'J. Chisenga', 'Computer Science', 'School of Natural Sciences', 3,
     'j.chisenga@dummy.unza', '+260971100018', '/img/students/jchisenga.jpg', 'QR-CS-007', 'ACTIVE', FALSE),
    ('2022004282', '1234567908', 'K. Musonda', 'Electrical Engineering', 'School of Engineering', 1,
     'k.musonda@dummy.unza', '+260971100019', '/img/students/kmusonda.jpg', 'QR-EE-005', 'ACTIVE', FALSE),
    ('2022004283', '1234567909', 'L. Chirwa', 'Education', 'School of Education', 4,
     'l.chirwa@dummy.unza', '+260971100020', '/img/students/lchirwa.jpg', 'QR-ED-005', 'ACTIVE', FALSE)
ON CONFLICT (computer_number) DO UPDATE
SET school = EXCLUDED.school;
