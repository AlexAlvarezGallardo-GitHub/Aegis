package com.aegis.common.domain.exception;

import java.time.Instant;
import java.util.UUID;

/**
 * Base runtime exception for all Aegis platform errors.
 */
public abstract class AegisException extends RuntimeException {

    private final String code;
    private final Instant timestamp;
    private final String errorId;

    /**
     * Creates a new exception with the given error code and message.
     *
     * @param code    the machine-readable error code
     * @param message the human-readable error message
     */
    protected AegisException(String code, String message) {
        super(message);
        this.code = code;
        this.timestamp = Instant.now();
        this.errorId = UUID.randomUUID().toString();
    }

    /**
     * Creates a new exception with the given error code, message, and cause.
     *
     * @param code    the machine-readable error code
     * @param message the human-readable error message
     * @param cause   the underlying cause of the error
     */
    protected AegisException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.timestamp = Instant.now();
        this.errorId = UUID.randomUUID().toString();
    }

    /**
     * @return the machine-readable error code
     */
    public String getCode() {
        return code;
    }

    /**
     * @return the instant when the exception was created
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * @return the unique error correlation id
     */
    public String getErrorId() {
        return errorId;
    }
}
