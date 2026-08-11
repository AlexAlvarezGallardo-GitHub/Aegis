package com.aegis.payment.domain.model;

/**
 * Lifecycle states of a {@link Transfer} aggregate.
 *
 * <p>State machine:
 * <pre>
 *   PENDING ─▶ FRAUD_CHECK ─▶ FUNDS_RESERVED ─▶ COMPLETED
 *       │            │               │
 *       └────────────┴───────────────┴──▶ FAILED
 *
 *   REVERSED exists for UC-007 (refund flow) and is unused in this scaffold.
 * </pre>
 */
public enum TransferStatus {
    PENDING,
    FRAUD_CHECK,
    FUNDS_RESERVED,
    COMPLETED,
    FAILED,
    REVERSED
}
