-- V10: Idempotency records retained for at least 24 hours.

CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,

    customer_id UUID NOT NULL,
    operation VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_fingerprint VARCHAR(128) NOT NULL,

    resource_id UUID,

    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_idempotency_records_customer
        FOREIGN KEY (customer_id)
            REFERENCES users(id) ON DELETE RESTRICT,

    CONSTRAINT uq_idempotency_records_key
        UNIQUE (customer_id, operation, idempotency_key),

    CONSTRAINT chk_idempotency_records_key
        CHECK (LENGTH(TRIM(idempotency_key)) > 0),

    CONSTRAINT chk_idempotency_records_expiry
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_idempotency_records_expiry
    ON idempotency_records(expires_at);
