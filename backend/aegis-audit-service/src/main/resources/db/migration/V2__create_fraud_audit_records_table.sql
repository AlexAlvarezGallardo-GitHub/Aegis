CREATE TABLE fraud_audit_records (
    id                  UUID PRIMARY KEY,
    assessment_id       UUID         NOT NULL,
    transaction_id      UUID         NOT NULL,
    transaction_type    VARCHAR(30)  NOT NULL,
    risk_score          INTEGER      NOT NULL,
    decision            VARCHAR(10)  NOT NULL,
    rules_evaluated     JSONB        NOT NULL,
    event_timestamp     TIMESTAMP    NOT NULL,
    ingested_at         TIMESTAMP    NOT NULL
);

CREATE INDEX idx_fraud_audit_assessment_id ON fraud_audit_records (assessment_id);
CREATE INDEX idx_fraud_audit_transaction_id ON fraud_audit_records (transaction_id);
