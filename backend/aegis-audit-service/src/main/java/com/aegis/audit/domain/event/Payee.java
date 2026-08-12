package com.aegis.audit.domain.event;

/**
 * Value object representing a payment payee.
 * <p>
 * Used for Kafka deserialization of the {@code payee} field
 * in {@link PaymentRequestedEvent} and {@link PaymentExecutedEvent}.
 * </p>
 *
 * @param name payee display name
 * @param id   payee identifier
 * @param type payee type (MERCHANT, INDIVIDUAL, SERVICE)
 */
public record Payee(String name, String id, String type) {
}
