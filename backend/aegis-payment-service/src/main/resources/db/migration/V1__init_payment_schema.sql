CREATE TABLE transfers (
    id                  UUID PRIMARY KEY,
    source_wallet_id    UUID             NOT NULL,
    dest_wallet_id      UUID             NOT NULL,
    user_id             UUID             NOT NULL,
    amount              NUMERIC(19, 2)   NOT NULL,
    currency            VARCHAR(3)       NOT NULL,
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

CREATE UNIQUE INDEX uk_transfers_source_wallet_reference
    ON transfers (source_wallet_id, reference);

CREATE INDEX idx_transfers_user_id ON transfers (user_id);
CREATE INDEX idx_transfers_status ON transfers (status);
CREATE INDEX idx_transfers_created_at ON transfers (created_at);

CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         TEXT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at    TIMESTAMP WITH TIME ZONE,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at);

CREATE TABLE processed_events (
    event_id       UUID         PRIMARY KEY,
    topic          VARCHAR(255) NOT NULL,
    partition      INTEGER      NOT NULL,
    offset         BIGINT       NOT NULL,
    processed_at   TIMESTAMP    NOT NULL
);

CREATE INDEX idx_processed_events_processed_at ON processed_events (processed_at);
