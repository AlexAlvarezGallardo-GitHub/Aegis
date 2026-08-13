package com.aegis.audit.domain.port.outbound;

import com.aegis.audit.domain.model.RefundAuditRecord;

/**
 * Outbound port for persisting and querying refund audit records.
 */
public interface RefundAuditRecordRepository {

    /**
     * Persists the given refund audit record.
     *
     * @param record the refund audit record to save
     * @return the saved refund audit record
     */
    RefundAuditRecord save(RefundAuditRecord record);
}
