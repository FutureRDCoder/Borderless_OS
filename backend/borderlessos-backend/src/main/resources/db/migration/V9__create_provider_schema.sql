-- V9: Local deterministic mock-provider attempts and inquiries.

CREATE TABLE provider_attempts (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL UNIQUE,

    provider VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(128) NOT NULL UNIQUE,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    request_fingerprint VARCHAR(128) NOT NULL,

    status VARCHAR(32) NOT NULL,
    scenario VARCHAR(48) NOT NULL,
    pending_outcome VARCHAR(32),

    last_inquiry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_provider_attempts_transfer
        FOREIGN KEY (transfer_id)
            REFERENCES transfers(id) ON DELETE RESTRICT,

    CONSTRAINT chk_provider_attempts_status
        CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED', 'UNKNOWN')),

    CONSTRAINT chk_provider_attempts_pending_outcome
        CHECK (pending_outcome IS NULL OR pending_outcome IN ('SUCCESS', 'FAILURE')),

    CONSTRAINT chk_provider_attempts_version
        CHECK (version >= 0)
);

CREATE INDEX idx_provider_attempts_status
    ON provider_attempts(status);

CREATE TABLE provider_inquiries (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    transfer_id UUID NOT NULL,

    actor_id UUID,
    actor_email VARCHAR(255) NOT NULL,

    result VARCHAR(32) NOT NULL,
    resolved_outcome VARCHAR(32),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_provider_inquiries_attempt
        FOREIGN KEY (attempt_id)
            REFERENCES provider_attempts(id) ON DELETE RESTRICT,

    CONSTRAINT fk_provider_inquiries_transfer
        FOREIGN KEY (transfer_id)
            REFERENCES transfers(id) ON DELETE RESTRICT,

    CONSTRAINT fk_provider_inquiries_actor
        FOREIGN KEY (actor_id)
            REFERENCES users(id) ON DELETE RESTRICT,

    CONSTRAINT chk_provider_inquiries_result
        CHECK (result IN ('INCONCLUSIVE', 'SUCCESS', 'FAILURE'))
);

CREATE INDEX idx_provider_inquiries_attempt
    ON provider_inquiries(attempt_id, created_at ASC);
