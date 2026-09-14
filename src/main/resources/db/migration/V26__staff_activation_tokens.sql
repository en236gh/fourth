CREATE TABLE IF NOT EXISTS public.staff_activation_token (
    activation_id BIGSERIAL PRIMARY KEY,
    staff_id INTEGER NOT NULL UNIQUE REFERENCES public.staff(staff_id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_staff_activation_token_staff
    ON public.staff_activation_token(staff_id);

CREATE INDEX IF NOT EXISTS idx_staff_activation_token_outstanding
    ON public.staff_activation_token(expires_at)
    WHERE used_at IS NULL;
