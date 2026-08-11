CREATE TABLE holds (
    id          UUID PRIMARY KEY,
    wallet_id   UUID NOT NULL REFERENCES wallets(id),
    amount      DECIMAL(19,2) NOT NULL,
    currency    VARCHAR(3) NOT NULL,
    reference   VARCHAR(255) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_holds_wallet_status ON holds (wallet_id, status);
CREATE UNIQUE INDEX uk_holds_reference ON holds (reference);
