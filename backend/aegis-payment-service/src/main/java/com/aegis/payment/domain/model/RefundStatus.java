package com.aegis.payment.domain.model;

/**
 * Lifecycle states of a {@link Refund} aggregate.
 *
 * <p>State machine:
 * <pre>
 *   PENDING ─▶ COMPLETED
 *       │
 *       └──▶ FAILED
 * </pre>
 */
public enum RefundStatus {
    PENDING,
    COMPLETED,
    FAILED
}
