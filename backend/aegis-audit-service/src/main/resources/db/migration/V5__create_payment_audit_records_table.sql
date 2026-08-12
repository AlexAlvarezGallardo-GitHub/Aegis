CREATE TABLE payment_audit_records (
    id                 UUID           PRIMARY KEY,
    event_id           UUID           NOT NULL,
    payment_id         UUID           NOT NULL,
    event_type         VARCHAR(20)    NOT NULL,
    wallet_id          UUID           NOT NULL,
    user_id            UUID           NOT NULL,
    amount             DECIMAL(19, 2) NOT NULL,
    currency           VARCHAR(3)     NOT NULL,
    payee_name         VARCHAR(255),
    failure_reason     VARCHAR(255),
    correlation_id     VARCHAR(255),
    event_timestamp    TIMESTAMP      NOT NULL,
    ingested_at        TIMESTAMP      NOT NULL
);

CREATE INDEX idx_payment_audit_payment_id ON payment_audit_records (payment_id);
CREATE INDEX idx_payment_audit_event_id ON payment_audit_records (event_id);
CREATE INDEX idx_payment_audit_event_timestamp ON payment_audit_records (event_timestamp);
