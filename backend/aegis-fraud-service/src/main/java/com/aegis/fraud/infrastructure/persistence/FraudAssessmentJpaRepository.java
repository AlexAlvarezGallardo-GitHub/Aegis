package com.aegis.fraud.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data repository for fraud assessment entities.
 */
public interface FraudAssessmentJpaRepository extends JpaRepository<FraudAssessmentJpaEntity, UUID> {
}
