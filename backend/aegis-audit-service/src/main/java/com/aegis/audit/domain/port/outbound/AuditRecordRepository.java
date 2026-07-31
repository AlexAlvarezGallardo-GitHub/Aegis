package com.aegis.audit.domain.port.outbound;

import com.aegis.audit.domain.model.AuditRecord;

/**
 * Outbound port for persisting and querying audit records.
 */
public interface AuditRecordRepository {

    /**
     * Persists the given audit record.
     *
     * @param record the audit record to save
     * @return the saved audit record
     */
    AuditRecord save(AuditRecord record);
}
