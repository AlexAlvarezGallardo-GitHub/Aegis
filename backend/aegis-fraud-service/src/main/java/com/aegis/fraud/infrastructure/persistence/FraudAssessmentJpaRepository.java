package com.aegis.fraud.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FraudAssessmentJpaRepository extends JpaRepository<FraudAssessmentJpaEntity, UUID> {
}
