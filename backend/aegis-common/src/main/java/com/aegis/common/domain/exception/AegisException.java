package com.aegis.common.domain.exception;

import java.time.Instant;
import java.util.UUID;

/**
 * Base runtime exception for all Aegis platform errors.
 */
public abstract class AegisException extends RuntimeException {

    private final String code;
    private final ErrorStatus errorStatus;
    private final Instant timestamp;
    private final String errorId;

    /**
     * Creates a new exception with the given error code and message.
     *
     * @param code    the machine-readable error code
     * @param message the human-readable error message
     */
    protected AegisException(String code, String message) {
        this(code, ErrorStatus.INTERNAL_SERVER_ERROR, message);
    }

    /**
     * Creates a new exception with the given error code, HTTP status, and message.
     *
     * @param code        the machine-readable error code
     * @param errorStatus the HTTP status to surface to clients
     * @param message     the human-readable error message
     */
    protected AegisException(String code, ErrorStatus errorStatus, String message) {
        super(message);
        this.code = code;
        this.errorStatus = errorStatus;
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
        this(code, ErrorStatus.INTERNAL_SERVER_ERROR, message, cause);
    }

    /**
     * Creates a new exception with the given error code, HTTP status, message, and cause.
     *
     * @param code        the machine-readable error code
     * @param errorStatus the HTTP status to surface to clients
     * @param message     the human-readable error message
     * @param cause       the underlying cause of the error
     */
    protected AegisException(String code, ErrorStatus errorStatus, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.errorStatus = errorStatus;
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
     * @return the HTTP status to surface to clients
     */
    public ErrorStatus getErrorStatus() {
        return errorStatus;
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
