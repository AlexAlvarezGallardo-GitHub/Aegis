package com.aegis.fraud.infrastructure.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FraudRuleJpaEntity - Persistence Entity")
class FraudRuleJpaEntityTest {

    @Nested
    @DisplayName("When creating a new rule entity")
    class WhenCreatingNewRuleEntity {

        @Test
        @DisplayName("Should initialize all fields correctly")
        void shouldInitializeAllFieldsCorrectly() {
            UUID id = UUID.randomUUID();
            FraudRuleJpaEntity entity = new FraudRuleJpaEntity(
                    id, "AMOUNT_THRESHOLD", FraudRuleJpaEntity.FraudRuleType.AMOUNT,
                    1000, 30, true);

            assertEquals(id, entity.getId());
            assertEquals("AMOUNT_THRESHOLD", entity.getName());
            assertEquals(FraudRuleJpaEntity.FraudRuleType.AMOUNT, entity.getType());
            assertEquals(1000, entity.getThreshold());
            assertEquals(30, entity.getWeight());
            assertTrue(entity.isEnabled());
        }

        @Test
        @DisplayName("Should handle disabled rule")
        void shouldHandleDisabledRule() {
            FraudRuleJpaEntity entity = new FraudRuleJpaEntity(
                    UUID.randomUUID(), "VELOCITY_CHECK", FraudRuleJpaEntity.FraudRuleType.VELOCITY,
                    5, 25, false);

            assertFalse(entity.isEnabled());
        }
    }

    @Nested
    @DisplayName("FraudRuleType enum")
    class FraudRuleTypeEnum {

        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveAllExpectedValues() {
            FraudRuleJpaEntity.FraudRuleType[] values = FraudRuleJpaEntity.FraudRuleType.values();
            assertEquals(4, values.length);
            assertEquals(FraudRuleJpaEntity.FraudRuleType.VELOCITY,
                    FraudRuleJpaEntity.FraudRuleType.valueOf("VELOCITY"));
            assertEquals(FraudRuleJpaEntity.FraudRuleType.AMOUNT,
                    FraudRuleJpaEntity.FraudRuleType.valueOf("AMOUNT"));
            assertEquals(FraudRuleJpaEntity.FraudRuleType.GEOGRAPHIC,
                    FraudRuleJpaEntity.FraudRuleType.valueOf("GEOGRAPHIC"));
            assertEquals(FraudRuleJpaEntity.FraudRuleType.TIME,
                    FraudRuleJpaEntity.FraudRuleType.valueOf("TIME"));
        }
    }
}
