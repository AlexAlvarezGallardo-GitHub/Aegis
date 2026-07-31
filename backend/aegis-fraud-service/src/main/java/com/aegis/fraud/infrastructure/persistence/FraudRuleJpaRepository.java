package com.aegis.fraud.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for fraud rule entities.
 */
public interface FraudRuleJpaRepository extends JpaRepository<FraudRuleJpaEntity, UUID> {

    List<FraudRuleJpaEntity> findByEnabledTrue();
}
