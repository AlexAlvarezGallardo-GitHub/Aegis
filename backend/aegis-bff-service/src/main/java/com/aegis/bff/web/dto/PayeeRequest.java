package com.aegis.bff.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Payee information for a payment request.
 *
 * @param name the payee name
 * @param id   the payee identifier
 * @param type the payee type (MERCHANT, INDIVIDUAL, SERVICE)
 */
public record PayeeRequest(
        @NotNull @NotBlank String name,
        @NotNull @NotBlank String id,
        @NotNull @NotBlank @Pattern(regexp = "MERCHANT|INDIVIDUAL|SERVICE",
                message = "type must be MERCHANT, INDIVIDUAL or SERVICE") String type) {
}
