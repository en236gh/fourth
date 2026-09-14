CREATE TABLE public.account_activation (
    activation_id BIGSERIAL PRIMARY KEY,
    computer_number VARCHAR(15) NOT NULL REFERENCES public.student(computer_number) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_account_activation_student
    ON public.account_activation(computer_number);

CREATE INDEX idx_account_activation_outstanding
    ON public.account_activation(expires_at)
    WHERE used_at IS NULL;
