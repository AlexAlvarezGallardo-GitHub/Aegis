package com.aegis.wallet.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Web-layer request body for creating a new wallet.
 *
 * @param currency the 3-letter ISO 4217 currency code
 */
public record CreateWalletRequest(
        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO 4217 code")
        String currency
) {
}
