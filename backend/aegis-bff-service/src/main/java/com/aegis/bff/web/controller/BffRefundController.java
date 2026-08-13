package com.aegis.bff.web.controller;

import com.aegis.bff.application.service.SessionJwtStore;
import com.aegis.bff.domain.port.PaymentExecutionClient;
import com.aegis.bff.domain.port.TokenValidator;
import com.aegis.bff.web.dto.CreateRefundRequest;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller that proxies refund operations to the Payment Service.
 */
@RestController
@RequestMapping("/api/bff/payments/{paymentId}/refund")
public class BffRefundController {

    private final PaymentExecutionClient paymentExecutionClient;
    private final SessionJwtStore sessionJwtStore;
    private final TokenValidator tokenValidator;

    public BffRefundController(PaymentExecutionClient paymentExecutionClient,
                               SessionJwtStore sessionJwtStore,
                               TokenValidator tokenValidator) {
        this.paymentExecutionClient = paymentExecutionClient;
        this.sessionJwtStore = sessionJwtStore;
        this.tokenValidator = tokenValidator;
    }

    @PostMapping
    public ResponseEntity<JsonNode> refundPayment(
            @PathVariable String paymentId,
            @Valid @RequestBody CreateRefundRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String accessToken = requireAccessToken();
        String userId = extractUserId(accessToken);
        String effectiveCorrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        JsonNode response = paymentExecutionClient.refundPayment(
                accessToken, userId, paymentId,
                request.amount(), request.reason(), request.reference(),
                effectiveCorrId);

        return ResponseEntity.ok(response);
    }

    private String requireAccessToken() {
        return sessionJwtStore.getAccessToken()
                .orElseThrow(() -> new IllegalStateException("Not authenticated"));
    }

    private String extractUserId(String accessToken) {
        Claims claims = tokenValidator.validate(accessToken);
        return claims.getSubject();
    }
}
