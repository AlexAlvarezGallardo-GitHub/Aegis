package com.aegis.wallet.domain.model;

/**
 * Lifecycle states of a funds hold.
 *
 * <pre>
 *   ACTIVE ──▶ SETTLED   (transfer completed)
 *   ACTIVE ──▶ RELEASED  (transfer cancelled / compensation)
 *   ACTIVE ──▶ EXPIRED   (TTL elapsed without settlement)
 * </pre>
 */
public enum HoldStatus {
    ACTIVE,
    SETTLED,
    RELEASED,
    EXPIRED
}
