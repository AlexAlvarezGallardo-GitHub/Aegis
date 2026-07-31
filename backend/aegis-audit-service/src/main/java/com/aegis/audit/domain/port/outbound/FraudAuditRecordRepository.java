package com.aegis.audit.domain.port.outbound;

import com.aegis.audit.domain.model.FraudAuditRecord;

/**
 * Outbound port for persisting and querying fraud audit records.
 */
public interface FraudAuditRecordRepository {

    /**
     * Persists the given fraud audit record.
     *
     * @param record the fraud audit record to save
     * @return the saved fraud audit record
     */
    FraudAuditRecord save(FraudAuditRecord record);
}
