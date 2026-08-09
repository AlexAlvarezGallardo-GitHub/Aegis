-- Unique partial index enforcing idempotency for deposits under concurrent
-- requests: a deposit reference may be used at most once per wallet.
-- Partial (WHERE reference IS NOT NULL) so opening/withdrawal entries that use
-- human descriptions are not constrained.
CREATE UNIQUE INDEX idx_ledger_entries_deposit_reference
    ON ledger_entries (wallet_id, reference)
    WHERE type = 'DEPOSIT' AND reference IS NOT NULL;
