CREATE TABLE balance_projections (
    id             UUID           PRIMARY KEY,
    wallet_id      UUID           NOT NULL UNIQUE,
    user_id        UUID           NOT NULL,
    balance        DECIMAL(19, 2) NOT NULL,
    currency       VARCHAR(3)     NOT NULL,
    last_updated   TIMESTAMP      NOT NULL
);
