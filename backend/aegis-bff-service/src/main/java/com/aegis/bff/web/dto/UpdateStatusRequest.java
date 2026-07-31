package com.aegis.bff.web.dto;

/**
 * Request body for updating a wallet's status.
 *
 * @param status the new wallet status (e.g. ACTIVE, FROZEN, CLOSED)
 */
public record UpdateStatusRequest(String status) {
}
