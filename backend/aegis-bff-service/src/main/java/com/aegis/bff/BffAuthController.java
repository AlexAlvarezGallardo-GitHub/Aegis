package com.aegis.bff;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bff/auth")
public class BffAuthController {

    private final BffService bffService;

    public BffAuthController(BffService bffService) {
        this.bffService = bffService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String effectiveCorrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        return ResponseEntity.ok(bffService.login(body.get("email"), body.get("password"), effectiveCorrId));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String effectiveCorrId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        return ResponseEntity.ok(bffService.refresh(effectiveCorrId));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        bffService.logout();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        return ResponseEntity.ok(bffService.getCurrentUser());
    }
}
