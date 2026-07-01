package com.aegis.common.domain.exception;

import java.time.Instant;
import java.util.UUID;

public abstract class AegisException extends RuntimeException {

    private final String code;
    private final Instant timestamp;
    private final String errorId;

    protected AegisException(String code, String message) {
        super(message);
        this.code = code;
        this.timestamp = Instant.now();
        this.errorId = UUID.randomUUID().toString();
    }

    protected AegisException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.timestamp = Instant.now();
        this.errorId = UUID.randomUUID().toString();
    }

    public String getCode() {
        return code;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getErrorId() {
        return errorId;
    }
}
