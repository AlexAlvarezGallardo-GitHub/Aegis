package com.aegis.fraud.infrastructure.persistence;

import com.aegis.fraud.domain.model.FraudRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudRuleRepositoryAdapter - Persistence Adapter")
class FraudRuleRepositoryAdapterTest {

    @Mock
    private FraudRuleJpaRepository jpaRepository;

    private FraudRuleRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FraudRuleRepositoryAdapter(jpaRepository);
    }

    @Nested
    @DisplayName("When finding enabled rules")
    class WhenFindingEnabledRules {

        @Test
        @DisplayName("Should return mapped domain rules from JPA entities")
        void shouldReturnMappedDomainRules() {
            // Arrange
            UUID ruleId1 = UUID.randomUUID();
            UUID ruleId2 = UUID.randomUUID();

            FraudRuleJpaEntity entity1 = new FraudRuleJpaEntity(
                    ruleId1, "AMOUNT_THRESHOLD", FraudRuleJpaEntity.FraudRuleType.AMOUNT,
                    1000, 30, true);
            FraudRuleJpaEntity entity2 = new FraudRuleJpaEntity(
                    ruleId2, "VELOCITY_CHECK", FraudRuleJpaEntity.FraudRuleType.VELOCITY,
                    5, 25, true);

            when(jpaRepository.findByEnabledTrue()).thenReturn(List.of(entity1, entity2));

            // Act
            List<FraudRule> rules = adapter.findEnabledRules();

            // Assert
            assertEquals(2, rules.size());

            FraudRule rule1 = rules.get(0);
            assertEquals(ruleId1, rule1.id());
            assertEquals("AMOUNT_THRESHOLD", rule1.name());
            assertEquals(FraudRule.RuleType.AMOUNT, rule1.type());
            assertEquals(1000, rule1.threshold());
            assertEquals(30, rule1.weight());
            assertTrue(rule1.enabled());

            FraudRule rule2 = rules.get(1);
            assertEquals(ruleId2, rule2.id());
            assertEquals("VELOCITY_CHECK", rule2.name());
            assertEquals(FraudRule.RuleType.VELOCITY, rule2.type());
            assertEquals(5, rule2.threshold());
            assertEquals(25, rule2.weight());
            assertTrue(rule2.enabled());
        }

        @Test
        @DisplayName("Should return empty list when no enabled rules exist")
        void shouldReturnEmptyListWhenNoEnabledRules() {
            // Arrange
            when(jpaRepository.findByEnabledTrue()).thenReturn(List.of());

            // Act
            List<FraudRule> rules = adapter.findEnabledRules();

            // Assert
            assertTrue(rules.isEmpty());
        }

        @Test
        @DisplayName("Should map all rule types correctly")
        void shouldMapAllRuleTypesCorrectly() {
            // Arrange
            FraudRuleJpaEntity velocityEntity = new FraudRuleJpaEntity(
                    UUID.randomUUID(), "VELOCITY", FraudRuleJpaEntity.FraudRuleType.VELOCITY,
                    5, 25, true);
            FraudRuleJpaEntity amountEntity = new FraudRuleJpaEntity(
                    UUID.randomUUID(), "AMOUNT", FraudRuleJpaEntity.FraudRuleType.AMOUNT,
                    1000, 30, true);
            FraudRuleJpaEntity geographicEntity = new FraudRuleJpaEntity(
                    UUID.randomUUID(), "GEOGRAPHIC", FraudRuleJpaEntity.FraudRuleType.GEOGRAPHIC,
                    0, 20, true);
            FraudRuleJpaEntity timeEntity = new FraudRuleJpaEntity(
                    UUID.randomUUID(), "TIME", FraudRuleJpaEntity.FraudRuleType.TIME,
                    0, 15, true);

            when(jpaRepository.findByEnabledTrue())
                    .thenReturn(List.of(velocityEntity, amountEntity, geographicEntity, timeEntity));

            // Act
            List<FraudRule> rules = adapter.findEnabledRules();

            // Assert
            assertEquals(4, rules.size());
            assertEquals(FraudRule.RuleType.VELOCITY, rules.get(0).type());
            assertEquals(FraudRule.RuleType.AMOUNT, rules.get(1).type());
            assertEquals(FraudRule.RuleType.GEOGRAPHIC, rules.get(2).type());
            assertEquals(FraudRule.RuleType.TIME, rules.get(3).type());
        }
    }
}
