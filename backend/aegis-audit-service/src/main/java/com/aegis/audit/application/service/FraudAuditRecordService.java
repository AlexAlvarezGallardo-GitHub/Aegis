package com.aegis.audit.application.service;

import com.aegis.audit.domain.model.FraudAuditRecord;
import com.aegis.audit.domain.port.outbound.FraudAuditRecordRepository;
import org.springframework.stereotype.Service;

/**
 * Application service that coordinates the persistence of fraud audit records.
 */
@Service
public class FraudAuditRecordService {

    private final FraudAuditRecordRepository fraudAuditRecordRepository;

    public FraudAuditRecordService(FraudAuditRecordRepository fraudAuditRecordRepository) {
        this.fraudAuditRecordRepository = fraudAuditRecordRepository;
    }

    /**
     * Persists the given fraud audit record.
     *
     * @param record the fraud audit record to save
     * @return the saved fraud audit record
     */
    public FraudAuditRecord save(FraudAuditRecord record) {
        return fraudAuditRecordRepository.save(record);
    }
}
