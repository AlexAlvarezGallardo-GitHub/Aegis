package com.aegis.payment.domain.model;

/**
 * Lifecycle states of a {@link Payment} aggregate.
 *
 * <p>State machine:
 * <pre>
 *   PENDING ─▶ PROCESSING ─▶ COMPLETED
 *       │            │
 *       └────────────┴──▶ FAILED
 *
 *   REFUNDED exists for UC-007 (refund flow) and is unused in this scaffold.
 * </pre>
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED
}
