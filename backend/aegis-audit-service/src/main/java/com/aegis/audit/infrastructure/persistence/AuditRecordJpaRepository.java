package com.aegis.audit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AuditRecordJpaEntity} entities.
 */
public interface AuditRecordJpaRepository extends JpaRepository<AuditRecordJpaEntity, UUID> {
}
