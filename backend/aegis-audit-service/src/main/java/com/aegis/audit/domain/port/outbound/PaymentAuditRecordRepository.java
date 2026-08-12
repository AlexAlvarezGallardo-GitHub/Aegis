package com.aegis.audit.domain.port.outbound;

import com.aegis.audit.domain.model.PaymentAuditRecord;

/**
 * Outbound port for persisting and querying payment audit records.
 */
public interface PaymentAuditRecordRepository {

    /**
     * Persists the given payment audit record.
     *
     * @param record the payment audit record to save
     * @return the saved payment audit record
     */
    PaymentAuditRecord save(PaymentAuditRecord record);
}
