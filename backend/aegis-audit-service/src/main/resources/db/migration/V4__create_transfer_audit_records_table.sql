CREATE TABLE transfer_audit_records (
    id                 UUID           PRIMARY KEY,
    event_id           UUID           NOT NULL,
    transfer_id        UUID           NOT NULL,
    event_type         VARCHAR(20)    NOT NULL,
    source_wallet_id   UUID           NOT NULL,
    dest_wallet_id     UUID           NOT NULL,
    user_id            UUID           NOT NULL,
    amount             DECIMAL(19, 2) NOT NULL,
    currency           VARCHAR(3)     NOT NULL,
    reference          VARCHAR(255),
    failure_reason     VARCHAR(255),
    correlation_id     VARCHAR(255),
    event_timestamp    TIMESTAMP      NOT NULL,
    ingested_at        TIMESTAMP      NOT NULL
);

CREATE INDEX idx_transfer_audit_transfer_id ON transfer_audit_records (transfer_id);
CREATE INDEX idx_transfer_audit_event_id ON transfer_audit_records (event_id);
CREATE INDEX idx_transfer_audit_event_timestamp ON transfer_audit_records (event_timestamp);
