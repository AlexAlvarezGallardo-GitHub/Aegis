package com.aegis.audit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link FraudAuditRecordJpaEntity} entities.
 */
public interface FraudAuditRecordJpaRepository extends JpaRepository<FraudAuditRecordJpaEntity, UUID> {
}
