package com.aegis.audit.infrastructure.persistence;

import com.aegis.audit.domain.model.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AuditRecord} entities.
 * <p>
 * Provides standard CRUD operations for audit records persisted in the database.
 * </p>
 */
@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, UUID> {
}
