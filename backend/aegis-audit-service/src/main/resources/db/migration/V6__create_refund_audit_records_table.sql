CREATE TABLE refund_audit_records (
    id                 UUID           PRIMARY KEY,
    event_id           UUID           NOT NULL,
    refund_id          UUID           NOT NULL,
    payment_id         UUID           NOT NULL,
    wallet_id          UUID           NOT NULL,
    user_id            UUID           NOT NULL,
    amount             DECIMAL(19, 2) NOT NULL,
    currency           VARCHAR(3)     NOT NULL,
    reason             VARCHAR(255),
    reference          VARCHAR(255)   NOT NULL,
    correlation_id     VARCHAR(255),
    event_timestamp    TIMESTAMP      NOT NULL,
    ingested_at        TIMESTAMP      NOT NULL
);

CREATE INDEX idx_refund_audit_refund_id ON refund_audit_records (refund_id);
CREATE INDEX idx_refund_audit_event_id ON refund_audit_records (event_id);
CREATE INDEX idx_refund_audit_event_timestamp ON refund_audit_records (event_timestamp);
