CREATE TABLE fraud_rules (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    type        VARCHAR(20)  NOT NULL,
    threshold   INTEGER      NOT NULL,
    weight      INTEGER      NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE fraud_assessments (
    id                UUID PRIMARY KEY,
    transaction_id    UUID         NOT NULL,
    transaction_type  VARCHAR(30)  NOT NULL,
    risk_score        INTEGER      NOT NULL,
    decision          VARCHAR(10)  NOT NULL,
    rules_evaluated   JSONB        NOT NULL,
    timestamp         TIMESTAMP    NOT NULL
);

CREATE INDEX idx_fraud_assessments_transaction_id ON fraud_assessments (transaction_id);
CREATE INDEX idx_fraud_assessments_timestamp ON fraud_assessments (timestamp);

INSERT INTO fraud_rules (id, name, type, threshold, weight, enabled) VALUES
    (gen_random_uuid(), 'VELOCITY_CHECK', 'VELOCITY', 5, 25, TRUE),
    (gen_random_uuid(), 'AMOUNT_THRESHOLD', 'AMOUNT', 1000, 30, TRUE),
    (gen_random_uuid(), 'GEOGRAPHIC_ANOMALY', 'GEOGRAPHIC', 0, 30, TRUE),
    (gen_random_uuid(), 'OFF_HOURS_TRANSACTION', 'TIME', 0, 15, TRUE);
