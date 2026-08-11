CREATE TABLE transfer_projections (
    id                 UUID           PRIMARY KEY,
    transfer_id        UUID           NOT NULL UNIQUE,
    source_wallet_id   UUID           NOT NULL,
    dest_wallet_id     UUID           NOT NULL,
    user_id            UUID           NOT NULL,
    amount             DECIMAL(19, 2) NOT NULL,
    currency           VARCHAR(3)     NOT NULL,
    status             VARCHAR(20)    NOT NULL,
    failure_reason     VARCHAR(255),
    event_timestamp    TIMESTAMP      NOT NULL
);

CREATE INDEX idx_transfer_projection_source_wallet_id ON transfer_projections (source_wallet_id);
CREATE INDEX idx_transfer_projection_dest_wallet_id ON transfer_projections (dest_wallet_id);
