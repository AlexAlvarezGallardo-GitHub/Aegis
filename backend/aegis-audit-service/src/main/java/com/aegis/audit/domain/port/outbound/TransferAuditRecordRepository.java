package com.aegis.audit.domain.port.outbound;

import com.aegis.audit.domain.model.TransferAuditRecord;

/**
 * Outbound port for persisting and querying transfer audit records.
 */
public interface TransferAuditRecordRepository {

    /**
     * Persists the given transfer audit record.
     *
     * @param record the transfer audit record to save
     * @return the saved transfer audit record
     */
    TransferAuditRecord save(TransferAuditRecord record);
}
