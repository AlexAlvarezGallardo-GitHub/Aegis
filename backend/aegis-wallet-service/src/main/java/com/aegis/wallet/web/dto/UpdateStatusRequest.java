package com.aegis.wallet.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Web-layer request body for updating a wallet status.
 *
 * @param status the target status (ACTIVE, FROZEN, or CLOSED)
 */
public record UpdateStatusRequest(
        @NotBlank(message = "Status is required")
        String status
) {
}
