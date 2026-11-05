-- V11: Customer-facing information requests for NEEDS_INFO transfers.

CREATE TABLE transfer_information_requests (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL,

    requested_by UUID,
    requested_by_email VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    responded_at TIMESTAMPTZ,
    responded_by UUID,
    responded_by_email VARCHAR(255),
    response_message VARCHAR(1000),

    CONSTRAINT fk_information_requests_transfer
        FOREIGN KEY (transfer_id)
            REFERENCES transfers(id) ON DELETE RESTRICT,

    CONSTRAINT fk_information_requests_requested_by
        FOREIGN KEY (requested_by)
            REFERENCES users(id) ON DELETE RESTRICT,

    CONSTRAINT fk_information_requests_responded_by
        FOREIGN KEY (responded_by)
            REFERENCES users(id) ON DELETE RESTRICT,

    CONSTRAINT chk_information_requests_message
        CHECK (LENGTH(TRIM(message)) > 0),

    CONSTRAINT chk_information_requests_response
        CHECK (
            (responded_at IS NULL
                AND responded_by IS NULL
                AND responded_by_email IS NULL
                AND response_message IS NULL)
            OR
            (responded_at IS NOT NULL
                AND responded_by IS NOT NULL
                AND responded_by_email IS NOT NULL
                AND LENGTH(TRIM(response_message)) > 0)
        )
);

CREATE INDEX idx_information_requests_transfer
    ON transfer_information_requests(transfer_id, created_at ASC);
