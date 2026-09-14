ALTER TABLE public.staff
    ADD COLUMN account_status VARCHAR(20);

UPDATE public.staff
SET account_status = CASE
    WHEN password_hash IS NOT NULL THEN 'ACTIVE'
    ELSE 'PENDING'
END;

ALTER TABLE public.staff
    ALTER COLUMN account_status SET NOT NULL,
    ALTER COLUMN account_status SET DEFAULT 'PENDING',
    ALTER COLUMN password_hash DROP NOT NULL,
    ADD CONSTRAINT staff_account_status_check
        CHECK (account_status IN ('PENDING', 'ACTIVE', 'SUSPENDED'));

ALTER TABLE public.student
    ADD COLUMN account_status VARCHAR(20);

UPDATE public.student
SET account_status = CASE
    WHEN account_activated = TRUE THEN 'ACTIVE'
    ELSE 'PENDING'
END;

ALTER TABLE public.student
    ALTER COLUMN account_status SET NOT NULL,
    ALTER COLUMN account_status SET DEFAULT 'PENDING',
    ADD CONSTRAINT student_account_status_check
        CHECK (account_status IN ('PENDING', 'ACTIVE', 'SUSPENDED'));

CREATE TABLE public.account_activation (
    activation_id BIGSERIAL PRIMARY KEY,
    staff_id INTEGER REFERENCES public.staff(staff_id) ON DELETE CASCADE,
    computer_number VARCHAR(15) REFERENCES public.student(computer_number) ON DELETE CASCADE,
    account_type VARCHAR(20) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT account_activation_type_check
        CHECK (account_type IN ('STAFF', 'STUDENT')),
    CONSTRAINT account_activation_subject_check
        CHECK (
            (account_type = 'STAFF' AND staff_id IS NOT NULL AND computer_number IS NULL)
            OR
            (account_type = 'STUDENT' AND staff_id IS NULL AND computer_number IS NOT NULL)
        )
);

CREATE INDEX idx_account_activation_staff
    ON public.account_activation(staff_id);
CREATE INDEX idx_account_activation_student
    ON public.account_activation(computer_number);
CREATE INDEX idx_account_activation_outstanding
    ON public.account_activation(expires_at)
    WHERE used_at IS NULL;
