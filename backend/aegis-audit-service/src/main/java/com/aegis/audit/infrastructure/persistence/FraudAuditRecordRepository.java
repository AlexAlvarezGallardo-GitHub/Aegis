package com.aegis.audit.infrastructure.persistence;

import com.aegis.audit.domain.model.FraudAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FraudAuditRecordRepository extends JpaRepository<FraudAuditRecord, UUID> {
}
