CREATE TABLE beneficiaries (

                               id UUID PRIMARY KEY,

                               customer_id UUID NOT NULL,

                               recipient_name VARCHAR(120) NOT NULL,

                               destination_country CHAR(2) NOT NULL,

                               destination_currency CHAR(3) NOT NULL,

                               account_reference VARCHAR(64) NOT NULL,

                               active BOOLEAN NOT NULL DEFAULT TRUE,

                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               version BIGINT NOT NULL DEFAULT 0,

                               CONSTRAINT fk_beneficiaries_customer
                                   FOREIGN KEY (customer_id)
                                       REFERENCES users(id)
                                       ON DELETE RESTRICT,

                               CONSTRAINT chk_beneficiaries_recipient_name
                                   CHECK (LENGTH(TRIM(recipient_name)) > 0),

                               CONSTRAINT chk_beneficiaries_country
                                   CHECK (destination_country ~ '^[A-Z]{2}$'),

    CONSTRAINT chk_beneficiaries_currency
        CHECK (destination_currency ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_beneficiaries_account_reference
        CHECK (LENGTH(TRIM(account_reference)) > 0),

    CONSTRAINT chk_beneficiaries_version
        CHECK (version >= 0)

);

CREATE INDEX idx_beneficiaries_customer_id
    ON beneficiaries(customer_id);

CREATE INDEX idx_beneficiaries_customer_active
    ON beneficiaries(customer_id, active);