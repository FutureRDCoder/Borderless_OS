CREATE TABLE transfers (
                           id UUID PRIMARY KEY,
                           customer_id UUID NOT NULL,
                           beneficiary_id UUID NOT NULL,
                           fx_quote_id UUID NOT NULL UNIQUE,

                           recipient_name VARCHAR(120) NOT NULL,
                           destination_country CHAR(2) NOT NULL,
                           destination_currency VARCHAR(3) NOT NULL,
                           beneficiary_account_reference VARCHAR(120) NOT NULL,

                           source_currency VARCHAR(3) NOT NULL,
                           source_amount NUMERIC(19,4) NOT NULL,
                           destination_amount NUMERIC(19,4) NOT NULL,
                           exchange_rate NUMERIC(24,12) NOT NULL,

                           status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',

                           submitted_at TIMESTAMPTZ NOT NULL,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           version BIGINT NOT NULL DEFAULT 0,

                           CONSTRAINT fk_transfer_customer
                               FOREIGN KEY (customer_id)
                                   REFERENCES users(id) ON DELETE RESTRICT,

                           CONSTRAINT fk_transfer_beneficiary
                               FOREIGN KEY (beneficiary_id)
                                   REFERENCES beneficiaries(id) ON DELETE RESTRICT,

                           CONSTRAINT fk_transfer_fx_quote
                               FOREIGN KEY (fx_quote_id)
                                   REFERENCES fx_quotes(id) ON DELETE RESTRICT,

                           CONSTRAINT chk_transfer_status CHECK (
                               status IN (
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
                               ),

                           CONSTRAINT chk_transfer_amounts CHECK (
                               source_amount > 0 AND destination_amount > 0
                               ),

                           CONSTRAINT chk_transfer_exchange_rate CHECK (
                               exchange_rate > 0
                               )
);

CREATE INDEX idx_transfers_customer_created
    ON transfers(customer_id, created_at DESC);

CREATE INDEX idx_transfers_status_created
    ON transfers(status, created_at ASC);

CREATE TABLE transfer_audit_events (
                                       id UUID PRIMARY KEY,
                                       transfer_id UUID NOT NULL,

                                       actor_id UUID,
                                       actor_email VARCHAR(255) NOT NULL,
                                       actor_role VARCHAR(32) NOT NULL,

                                       event_type VARCHAR(64) NOT NULL,
                                       previous_status VARCHAR(32),
                                       resulting_status VARCHAR(32) NOT NULL,

                                       reason VARCHAR(1000),
                                       safe_summary VARCHAR(500) NOT NULL,

                                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_transfer_audit_transfer
                                           FOREIGN KEY (transfer_id)
                                               REFERENCES transfers(id) ON DELETE RESTRICT,

                                       CONSTRAINT fk_transfer_audit_actor
                                           FOREIGN KEY (actor_id)
                                               REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX idx_transfer_audit_history
    ON transfer_audit_events(transfer_id, created_at ASC);