package com.aegis.fraud.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "fraud_rules")
public class FraudRuleJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FraudRuleType type;

    @Column(nullable = false)
    private int threshold;

    @Column(nullable = false)
    private int weight;

    @Column(nullable = false)
    private boolean enabled;

    protected FraudRuleJpaEntity() {}

    public FraudRuleJpaEntity(UUID id, String name, FraudRuleType type,
                              int threshold, int weight, boolean enabled) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.threshold = threshold;
        this.weight = weight;
        this.enabled = enabled;
    }

    public enum FraudRuleType {
        VELOCITY,
        AMOUNT,
        GEOGRAPHIC,
        TIME
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public FraudRuleType getType() {
        return type;
    }

    public int getThreshold() {
        return threshold;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
