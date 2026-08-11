package com.aegis.bff.web.controller;

import com.aegis.bff.application.service.SessionJwtStore;
import com.aegis.bff.domain.port.PaymentClient;
import com.aegis.bff.domain.port.TokenValidator;
import com.aegis.bff.web.dto.CreateTransferRequest;
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
 * REST controller that proxies transfer operations to the Payment Service.
 */
@RestController
@RequestMapping("/api/bff/transfers")
public class BffTransferController {

    private final PaymentClient paymentClient;
    private final SessionJwtStore sessionJwtStore;
    private final TokenValidator tokenValidator;

    public BffTransferController(PaymentClient paymentClient,
                                 SessionJwtStore sessionJwtStore,
                                 TokenValidator tokenValidator) {
        this.paymentClient = paymentClient;
        this.sessionJwtStore = sessionJwtStore;
        this.tokenValidator = tokenValidator;
    }

    @PostMapping
    public ResponseEntity<JsonNode> transferFunds(
            @Valid @RequestBody CreateTransferRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String accessToken = requireAccessToken();
        String userId = extractUserId(accessToken);
        String effectiveCorrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        JsonNode response = paymentClient.transferFunds(
                accessToken, userId,
                request.sourceWalletId().toString(),
                request.destWalletId().toString(),
                request.amount(),
                request.currency(),
                request.description(),
                request.reference(),
                effectiveCorrId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<JsonNode> getTransfer(
            @PathVariable String transferId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String accessToken = requireAccessToken();
        String userId = extractUserId(accessToken);
        String effectiveCorrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        JsonNode response = paymentClient.getTransfer(accessToken, userId, transferId, effectiveCorrId);
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
