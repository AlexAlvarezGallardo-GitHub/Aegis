package com.aegis.bff.web.controller;

import com.aegis.bff.application.service.SessionJwtStore;
import com.aegis.bff.domain.port.TokenValidator;
import com.aegis.bff.domain.port.WalletClient;
import com.aegis.bff.web.dto.AdjustBalanceRequest;
import com.aegis.bff.web.dto.CreateWalletRequest;
import com.aegis.bff.web.dto.DepositFundsRequest;
import com.aegis.bff.web.dto.UpdateStatusRequest;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller that proxies wallet operations to the Wallet Service.
 */
@RestController
@RequestMapping("/api/bff/wallets")
public class BffWalletController {

    private final WalletClient walletClient;
    private final SessionJwtStore sessionJwtStore;
    private final TokenValidator tokenValidator;

    public BffWalletController(WalletClient walletClient,
                               SessionJwtStore sessionJwtStore,
                               TokenValidator tokenValidator) {
        this.walletClient = walletClient;
        this.sessionJwtStore = sessionJwtStore;
        this.tokenValidator = tokenValidator;
    }

    @PostMapping
    public ResponseEntity<JsonNode> createWallet(
            @RequestBody CreateWalletRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String accessToken = requireAccessToken();
        String userId = extractUserId(accessToken);
        String effectiveCorrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        JsonNode response = walletClient.createWallet(accessToken, userId,
                request.currency(), effectiveCorrId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<JsonNode> listWallets() {
        String accessToken = requireAccessToken();
        String userId = extractUserId(accessToken);

        JsonNode response = walletClient.listWallets(accessToken, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<JsonNode> getWallet(@PathVariable String walletId) {
        String accessToken = requireAccessToken();
        String userId = extractUserId(accessToken);

        JsonNode response = walletClient.getWallet(accessToken, userId, walletId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{walletId}/balance")
    public ResponseEntity<JsonNode> adjustBalance(
            @PathVariable String walletId,
            @RequestBody AdjustBalanceRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String accessToken = requireAccessToken();
        String userId = extractUserId(accessToken);
        String effectiveCorrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        JsonNode response = walletClient.adjustBalance(accessToken, userId, walletId,
                request.type(), request.amount(), request.reason(), effectiveCorrId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{walletId}/deposits")
    public ResponseEntity<JsonNode> depositFunds(
            @PathVariable String walletId,
            @RequestBody DepositFundsRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String accessToken = requireAccessToken();
        String userId = extractUserId(accessToken);
        String effectiveCorrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        JsonNode response = walletClient.depositFunds(accessToken, userId, walletId,
                request.amount(), request.currency(), request.source(), request.reference(),
                effectiveCorrId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{walletId}/status")
    public ResponseEntity<JsonNode> updateStatus(
            @PathVariable String walletId,
            @RequestBody UpdateStatusRequest request) {

        String accessToken = requireAccessToken();
        String userId = extractUserId(accessToken);

        JsonNode response = walletClient.updateStatus(accessToken, userId, walletId,
                request.status());

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
