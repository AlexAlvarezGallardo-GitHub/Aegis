CREATE TABLE processed_events (
    event_id       UUID         PRIMARY KEY,
    topic          VARCHAR(255) NOT NULL,
    partition      INTEGER      NOT NULL,
    offset         BIGINT       NOT NULL,
    processed_at   TIMESTAMP    NOT NULL
);

CREATE INDEX idx_processed_events_processed_at ON processed_events (processed_at);
