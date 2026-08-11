package com.aegis.audit.application.service;

import com.aegis.audit.domain.model.TransferAuditRecord;
import com.aegis.audit.domain.port.outbound.TransferAuditRecordRepository;
import org.springframework.stereotype.Service;

/**
 * Application service that coordinates the persistence of transfer audit records.
 */
@Service
public class TransferAuditRecordService {

    private final TransferAuditRecordRepository transferAuditRecordRepository;

    public TransferAuditRecordService(TransferAuditRecordRepository transferAuditRecordRepository) {
        this.transferAuditRecordRepository = transferAuditRecordRepository;
    }

    /**
     * Persists the given transfer audit record.
     *
     * @param record the transfer audit record to save
     * @return the saved transfer audit record
     */
    public TransferAuditRecord save(TransferAuditRecord record) {
        return transferAuditRecordRepository.save(record);
    }
}
