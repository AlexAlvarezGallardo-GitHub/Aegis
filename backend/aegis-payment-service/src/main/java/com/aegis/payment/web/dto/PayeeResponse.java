package com.aegis.payment.web.dto;

/**
 * Web-layer response object for the payee within a payment response.
 */
public record PayeeResponse(
        String name,
        String id,
        String type
) {}
