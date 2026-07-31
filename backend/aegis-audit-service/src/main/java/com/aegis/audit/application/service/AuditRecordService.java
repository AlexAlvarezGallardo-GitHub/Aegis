package com.aegis.audit.application.service;

import com.aegis.audit.domain.model.AuditRecord;
import com.aegis.audit.domain.port.outbound.AuditRecordRepository;
import org.springframework.stereotype.Service;

/**
 * Application service that coordinates the persistence of audit records.
 */
@Service
public class AuditRecordService {

    private final AuditRecordRepository auditRecordRepository;

    public AuditRecordService(AuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    /**
     * Persists the given audit record.
     *
     * @param record the audit record to save
     * @return the saved audit record
     */
    public AuditRecord save(AuditRecord record) {
        return auditRecordRepository.save(record);
    }
}
