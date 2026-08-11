package com.aegis.common.domain.exception;

/**
 * HTTP status codes surfaced by Aegis domain errors.
 *
 * <p>Kept free of any framework dependency so the domain layer stays hexagonal.</p>
 */
public enum ErrorStatus {
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    UNPROCESSABLE_ENTITY(422),
    INTERNAL_SERVER_ERROR(500),
    SERVICE_UNAVAILABLE(503);

    private final int code;

    ErrorStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
