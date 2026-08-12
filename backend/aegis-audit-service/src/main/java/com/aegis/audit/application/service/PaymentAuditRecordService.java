package com.aegis.audit.application.service;

import com.aegis.audit.domain.model.PaymentAuditRecord;
import com.aegis.audit.domain.port.outbound.PaymentAuditRecordRepository;
import org.springframework.stereotype.Service;

/**
 * Application service that coordinates the persistence of payment audit records.
 */
@Service
public class PaymentAuditRecordService {

    private final PaymentAuditRecordRepository paymentAuditRecordRepository;

    public PaymentAuditRecordService(PaymentAuditRecordRepository paymentAuditRecordRepository) {
        this.paymentAuditRecordRepository = paymentAuditRecordRepository;
    }

    /**
     * Persists the given payment audit record.
     *
     * @param record the payment audit record to save
     * @return the saved payment audit record
     */
    public PaymentAuditRecord save(PaymentAuditRecord record) {
        return paymentAuditRecordRepository.save(record);
    }
}
