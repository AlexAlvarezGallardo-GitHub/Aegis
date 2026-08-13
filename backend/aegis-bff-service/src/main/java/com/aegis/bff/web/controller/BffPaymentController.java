package com.aegis.bff.web.controller;

import com.aegis.bff.application.service.SessionJwtStore;
import com.aegis.bff.domain.port.PaymentExecutionClient;
import com.aegis.bff.domain.port.TokenValidator;
import com.aegis.bff.web.dto.CreatePaymentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller that proxies payment operations to the Payment Service.
 */
@RestController
@RequestMapping("/api/bff/payments")
public class BffPaymentController {

    private final PaymentExecutionClient paymentExecutionClient;
    private final SessionJwtStore sessionJwtStore;
    private final TokenValidator tokenValidator;

    public BffPaymentController(PaymentExecutionClient paymentExecutionClient,
                                SessionJwtStore sessionJwtStore,
                                TokenValidator tokenValidator) {
        this.paymentExecutionClient = paymentExecutionClient;
        this.sessionJwtStore = sessionJwtStore;
        this.tokenValidator = tokenValidator;
    }

    @PostMapping
    public ResponseEntity<JsonNode> executePayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String accessToken = requireAccessToken();
        String userId = extractUserId(accessToken);
        String effectiveCorrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        JsonNode response = paymentExecutionClient.executePayment(
                accessToken, userId,
                request.walletId().toString(),
                request.amount(),
                request.currency(),
                request.payee().name(),
                request.payee().id(),
                request.payee().type(),
                request.description(),
                request.reference(),
                effectiveCorrId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<JsonNode> getPayment(
            @PathVariable String paymentId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String accessToken = requireAccessToken();
        String userId = extractUserId(accessToken);
        String effectiveCorrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        JsonNode response = paymentExecutionClient.getPayment(accessToken, userId, paymentId, effectiveCorrId);
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
