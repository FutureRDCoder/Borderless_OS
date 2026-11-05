-- V7: Persistent, editable DRAFT transfers.
-- A quote is reserved only after a successful submission, not while a
-- transfer remains a draft.

ALTER TABLE transfers
    DROP CONSTRAINT IF EXISTS transfers_fx_quote_id_key;

ALTER TABLE transfers
    DROP CONSTRAINT IF EXISTS chk_transfer_status;

ALTER TABLE transfers
    ADD CONSTRAINT chk_transfer_status CHECK (
        status IN (
            'DRAFT',
            'SUBMITTED',
            'UNDER_REVIEW',
            'NEEDS_INFO',
            'COMPLIANCE_HOLD',
            'READY_FOR_PROVIDER',
            'PROCESSING',
            'UNKNOWN',
            'SUCCEEDED',
            'FAILED',
            'REJECTED'
        )
    );

ALTER TABLE transfers
    ALTER COLUMN submitted_at DROP NOT NULL;

ALTER TABLE transfers
    ADD COLUMN quote_source VARCHAR(32) NOT NULL DEFAULT 'MOCK';

CREATE UNIQUE INDEX uq_transfers_fx_quote_submitted
    ON transfers(fx_quote_id)
    WHERE status <> 'DRAFT';
