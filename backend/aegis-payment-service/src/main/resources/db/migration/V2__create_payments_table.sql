CREATE TABLE payments (
    id                  UUID PRIMARY KEY,
    wallet_id           UUID             NOT NULL,
    user_id             UUID             NOT NULL,
    amount              NUMERIC(19, 2)   NOT NULL,
    currency            VARCHAR(3)       NOT NULL,
    payee_name          VARCHAR(255)     NOT NULL,
    payee_id            VARCHAR(255)     NOT NULL,
    payee_type          VARCHAR(20)      NOT NULL,
    description         VARCHAR(500),
    reference           VARCHAR(255)     NOT NULL,
    status              VARCHAR(20)      NOT NULL,
    fraud_assessment_id UUID,
    hold_id             UUID,
    failure_reason      VARCHAR(500),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at        TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uk_payments_wallet_reference
    ON payments (wallet_id, reference);

CREATE INDEX idx_payments_user_id ON payments (user_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_created_at ON payments (created_at);
