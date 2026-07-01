package com.aegis.identity.domain.model;

import com.aegis.common.util.UuidV7Generator;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "UserId value must not be null");
    }

    public static UserId generate() {
        return new UserId(UuidV7Generator.generate());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
