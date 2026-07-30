package com.aegis.bff;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bff/wallets")
public class BffWalletController {

    private final RestClient restClient;
    private final SessionJwtStore sessionJwtStore;

    public BffWalletController(BffProperties bffProperties,
                                SessionJwtStore sessionJwtStore) {
        this.restClient = RestClient.builder().baseUrl(bffProperties.getWalletService().getUrl()).build();
        this.sessionJwtStore = sessionJwtStore;
    }

    @PostMapping
    public ResponseEntity<JsonNode> createWallet(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String accessToken = sessionJwtStore.getAccessToken()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));

        String userId = extractUserId(accessToken);
        String effectiveCorrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        JsonNode response = restClient.post()
                .uri("/api/v1/wallets")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .header("X-Correlation-Id", effectiveCorrId)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<JsonNode> listWallets() {
        String accessToken = sessionJwtStore.getAccessToken()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));

        String userId = extractUserId(accessToken);

        JsonNode response = restClient.get()
                .uri("/api/v1/wallets")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .retrieve()
                .body(JsonNode.class);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<JsonNode> getWallet(@PathVariable String walletId) {
        String accessToken = sessionJwtStore.getAccessToken()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));

        String userId = extractUserId(accessToken);

        JsonNode response = restClient.get()
                .uri("/api/v1/wallets/{walletId}", walletId)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .retrieve()
                .body(JsonNode.class);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{walletId}/balance")
    public ResponseEntity<JsonNode> adjustBalance(
            @PathVariable String walletId,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String accessToken = sessionJwtStore.getAccessToken()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));

        String userId = extractUserId(accessToken);
        String effectiveCorrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        JsonNode response = restClient.patch()
                .uri("/api/v1/wallets/{walletId}/balance", walletId)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .header("X-Correlation-Id", effectiveCorrId)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{walletId}/status")
    public ResponseEntity<JsonNode> updateStatus(
            @PathVariable String walletId,
            @RequestBody Map<String, String> body) {

        String accessToken = sessionJwtStore.getAccessToken()
                .orElseThrow(() -> new RuntimeException("Not authenticated"));

        String userId = extractUserId(accessToken);

        JsonNode response = restClient.patch()
                .uri("/api/v1/wallets/{walletId}/status", walletId)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Id", userId)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        return ResponseEntity.ok(response);
    }

    private String extractUserId(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readTree(payload).get("sub").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract user id from token", e);
        }
    }
}
