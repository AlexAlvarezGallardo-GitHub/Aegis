package com.aegis.fraud.domain.model;

import java.util.UUID;

public record FraudRule(
        UUID id,
        String name,
        RuleType type,
        int threshold,
        int weight,
        boolean enabled
) {

    public enum RuleType {
        VELOCITY,
        AMOUNT,
        GEOGRAPHIC,
        TIME
    }

    public static FraudRule create(String name, RuleType type, int threshold, int weight) {
        return new FraudRule(UUID.randomUUID(), name, type, threshold, weight, true);
    }
}
