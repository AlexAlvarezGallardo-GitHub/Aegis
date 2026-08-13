CREATE TABLE refunds (
    id              UUID PRIMARY KEY,
    payment_id      UUID             NOT NULL REFERENCES payments(id),
    wallet_id       UUID             NOT NULL,
    user_id         UUID             NOT NULL,
    amount          NUMERIC(19, 2)   NOT NULL,
    currency        VARCHAR(3)       NOT NULL,
    reason          VARCHAR(255),
    reference       VARCHAR(255)     NOT NULL,
    status          VARCHAR(20)      NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at    TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uk_refunds_reference ON refunds (reference);
CREATE INDEX idx_refunds_payment_id ON refunds (payment_id);
CREATE INDEX idx_refunds_user_id ON refunds (user_id);
CREATE INDEX idx_refunds_status ON refunds (status);
