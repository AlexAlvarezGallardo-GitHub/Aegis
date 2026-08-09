package com.aegis.wallet.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Web-layer request body for reversing a previously applied deposit.
 *
 * @param reference a unique idempotency reference for the reversal
 */
public record ReversalRequest(
        @NotBlank(message = "Reference is required")
        String reference
) {
}
