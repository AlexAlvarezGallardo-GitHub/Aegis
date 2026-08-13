package com.aegis.audit.application.service;

import com.aegis.audit.domain.model.RefundAuditRecord;
import com.aegis.audit.domain.port.outbound.RefundAuditRecordRepository;
import org.springframework.stereotype.Service;

/**
 * Application service that coordinates the persistence of refund audit records.
 */
@Service
public class RefundAuditRecordService {

    private final RefundAuditRecordRepository refundAuditRecordRepository;

    public RefundAuditRecordService(RefundAuditRecordRepository refundAuditRecordRepository) {
        this.refundAuditRecordRepository = refundAuditRecordRepository;
    }

    /**
     * Persists the given refund audit record.
     *
     * @param record the refund audit record to save
     * @return the saved refund audit record
     */
    public RefundAuditRecord save(RefundAuditRecord record) {
        return refundAuditRecordRepository.save(record);
    }
}
