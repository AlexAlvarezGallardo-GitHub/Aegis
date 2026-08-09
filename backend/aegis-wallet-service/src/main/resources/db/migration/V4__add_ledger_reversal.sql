ALTER TABLE ledger_entries
    ADD COLUMN reversal_of UUID;

CREATE INDEX idx_ledger_entries_reversal_of ON ledger_entries (reversal_of);
