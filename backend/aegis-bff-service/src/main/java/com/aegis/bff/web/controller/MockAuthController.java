package com.aegis.bff.web.controller;

import com.aegis.bff.application.service.MockLoginService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Development-only controller that exposes a mock login endpoint.
 */
@Profile("dev")
@RestController
@RequestMapping("/api/bff/auth")
public class MockAuthController {

    private final MockLoginService mockLoginService;

    public MockAuthController(MockLoginService mockLoginService) {
        this.mockLoginService = mockLoginService;
    }

    @PostMapping("/mock-login")
    public ResponseEntity<Map<String, Object>> mockLogin() {
        return ResponseEntity.ok(mockLoginService.mockLogin());
    }
}
