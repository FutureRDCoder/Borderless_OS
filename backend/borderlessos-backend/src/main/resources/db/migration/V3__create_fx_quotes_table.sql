
-- V3: Create FX quotes table
-- Stores indicative, educational FX quotes.
-- This table does not facilitate real-money transactions.

CREATE TABLE fx_quotes (
                           id UUID PRIMARY KEY,

                           customer_id UUID NOT NULL,

                           source_currency CHAR(3) NOT NULL,
                           destination_currency CHAR(3) NOT NULL,

                           source_amount NUMERIC(19, 4) NOT NULL,
                           exchange_rate NUMERIC(24, 12) NOT NULL,
                           destination_amount NUMERIC(19, 4) NOT NULL,

                           provider VARCHAR(32) NOT NULL,
                           rate_date DATE NOT NULL,
                           rate_retrieved_at TIMESTAMPTZ NOT NULL,

                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           expires_at TIMESTAMPTZ NOT NULL,
                           consumed_at TIMESTAMPTZ,

                           version BIGINT NOT NULL DEFAULT 0,

                           CONSTRAINT fk_fx_quotes_customer
                               FOREIGN KEY (customer_id)
                                   REFERENCES users(id)
                                   ON DELETE RESTRICT,

                           CONSTRAINT chk_fx_quotes_source_currency
                               CHECK (source_currency ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_fx_quotes_destination_currency
        CHECK (destination_currency ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_fx_quotes_different_currencies
        CHECK (source_currency <> destination_currency),

    CONSTRAINT chk_fx_quotes_source_amount
        CHECK (source_amount > 0),

    CONSTRAINT chk_fx_quotes_exchange_rate
        CHECK (exchange_rate > 0),

    CONSTRAINT chk_fx_quotes_destination_amount
        CHECK (destination_amount > 0),

    CONSTRAINT chk_fx_quotes_provider
        CHECK (provider IN ('frankfurter', 'mock')),

    CONSTRAINT chk_fx_quotes_expiry
        CHECK (expires_at > created_at),

    CONSTRAINT chk_fx_quotes_consumed_at
        CHECK (consumed_at IS NULL OR consumed_at >= created_at),

    CONSTRAINT chk_fx_quotes_version
        CHECK (version >= 0)
);

-- Supports customer-specific quote history.
CREATE INDEX idx_fx_quotes_customer_created_at
    ON fx_quotes (customer_id, created_at DESC);

-- Supports finding expired quotes for future cleanup.
CREATE INDEX idx_fx_quotes_expires_at
    ON fx_quotes (expires_at);