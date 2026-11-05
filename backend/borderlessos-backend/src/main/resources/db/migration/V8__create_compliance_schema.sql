-- V8: Compliance holds and append-only compliance decisions.

CREATE TABLE compliance_holds (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL,

    status VARCHAR(16) NOT NULL,
    trigger_type VARCHAR(64) NOT NULL,
    trigger_rule_id VARCHAR(64) NOT NULL,
    trigger_summary VARCHAR(500) NOT NULL,

    opened_at TIMESTAMPTZ NOT NULL,
    opened_by UUID,
    opened_by_email VARCHAR(255) NOT NULL,

    cleared_at TIMESTAMPTZ,
    cleared_by UUID,
    cleared_by_email VARCHAR(255),
    clearance_reason VARCHAR(1000),

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_compliance_holds_transfer
        FOREIGN KEY (transfer_id)
            REFERENCES transfers(id) ON DELETE RESTRICT,

    CONSTRAINT fk_compliance_holds_opened_by
        FOREIGN KEY (opened_by)
            REFERENCES users(id) ON DELETE RESTRICT,

    CONSTRAINT fk_compliance_holds_cleared_by
        FOREIGN KEY (cleared_by)
            REFERENCES users(id) ON DELETE RESTRICT,

    CONSTRAINT chk_compliance_holds_status
        CHECK (status IN ('ACTIVE', 'CLEARED')),

    CONSTRAINT chk_compliance_holds_trigger_summary
        CHECK (LENGTH(TRIM(trigger_summary)) > 0),

    CONSTRAINT chk_compliance_holds_clearance
        CHECK (
            (status = 'ACTIVE'
                AND cleared_at IS NULL
                AND cleared_by IS NULL
                AND cleared_by_email IS NULL
                AND clearance_reason IS NULL)
            OR
            (status = 'CLEARED'
                AND cleared_at IS NOT NULL
                AND cleared_by IS NOT NULL
                AND cleared_by_email IS NOT NULL
                AND LENGTH(TRIM(clearance_reason)) > 0)
        ),

    CONSTRAINT chk_compliance_holds_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX uq_compliance_holds_active_transfer
    ON compliance_holds(transfer_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_compliance_holds_status_opened
    ON compliance_holds(status, opened_at ASC);

CREATE TABLE compliance_decisions (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL,
    hold_id UUID,

    decision_type VARCHAR(32) NOT NULL,
    actor_id UUID,
    actor_email VARCHAR(255) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,

    reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_compliance_decisions_transfer
        FOREIGN KEY (transfer_id)
            REFERENCES transfers(id) ON DELETE RESTRICT,

    CONSTRAINT fk_compliance_decisions_hold
        FOREIGN KEY (hold_id)
            REFERENCES compliance_holds(id) ON DELETE RESTRICT,

    CONSTRAINT fk_compliance_decisions_actor
        FOREIGN KEY (actor_id)
            REFERENCES users(id) ON DELETE RESTRICT,

    CONSTRAINT chk_compliance_decisions_type
        CHECK (decision_type IN (
            'HOLD_TRIGGERED',
            'HOLD_MAINTAINED',
            'HOLD_CLEARED',
            'HOLD_REPLACED'
        ))
);

CREATE INDEX idx_compliance_decisions_transfer
    ON compliance_decisions(transfer_id, created_at ASC);
