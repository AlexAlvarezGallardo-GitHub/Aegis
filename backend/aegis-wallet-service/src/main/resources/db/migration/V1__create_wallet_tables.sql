CREATE TABLE wallets (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    balance         DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    currency        VARCHAR(3) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);

CREATE TABLE ledger_entries (
    id              UUID PRIMARY KEY,
    wallet_id       UUID NOT NULL REFERENCES wallets(id),
    type            VARCHAR(20) NOT NULL,
    amount          DECIMAL(19, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    reference       VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ledger_entries_wallet_id ON ledger_entries (wallet_id);

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
