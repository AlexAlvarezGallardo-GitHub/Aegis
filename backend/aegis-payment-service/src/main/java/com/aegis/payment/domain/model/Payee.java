package com.aegis.payment.domain.model;

import java.util.Objects;

/**
 * Value object representing the recipient of a payment.
 *
 * @param name payee display name
 * @param id   payee identifier (e.g. merchant id)
 * @param type the payee type
 */
public record Payee(String name, String id, PayeeType type) {

    public Payee {
        Objects.requireNonNull(name, "payee name must not be null");
        Objects.requireNonNull(id, "payee id must not be null");
        Objects.requireNonNull(type, "payee type must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("payee name must not be blank");
        }
        if (id.isBlank()) {
            throw new IllegalArgumentException("payee id must not be blank");
        }
    }
}
