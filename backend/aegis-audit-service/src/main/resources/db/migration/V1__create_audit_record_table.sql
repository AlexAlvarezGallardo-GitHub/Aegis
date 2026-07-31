CREATE TABLE audit_records (
    id              UUID            PRIMARY KEY,
    wallet_id       UUID            NOT NULL,
    user_id         UUID            NOT NULL,
    amount          DECIMAL(19, 2)  NOT NULL,
    currency        VARCHAR(3)      NOT NULL,
    source          VARCHAR(50),
    reference       VARCHAR(255),
    new_balance     DECIMAL(19, 2)  NOT NULL,
    event_timestamp TIMESTAMP       NOT NULL,
    ingested_at     TIMESTAMP       NOT NULL,
    correlation_id  VARCHAR(255)
);

CREATE INDEX idx_audit_records_wallet_id ON audit_records (wallet_id);
CREATE INDEX idx_audit_records_user_id ON audit_records (user_id);
CREATE INDEX idx_audit_records_event_timestamp ON audit_records (event_timestamp);
CREATE INDEX idx_audit_records_correlation_id ON audit_records (correlation_id);
