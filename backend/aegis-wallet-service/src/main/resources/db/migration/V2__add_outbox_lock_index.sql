ALTER INDEX idx_outbox_status_created RENAME TO idx_outbox_status_created_at;

-- Add partial index for efficient pessimistic lock queries on PENDING events
CREATE INDEX idx_outbox_pending_created_for_update
    ON outbox_events (created_at ASC)
    WHERE status = 'PENDING';
