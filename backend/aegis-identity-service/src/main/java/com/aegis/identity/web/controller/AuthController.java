package com.aegis.identity.web.controller;

import com.aegis.identity.application.dto.AuthenticateUserCommand;
import com.aegis.identity.application.dto.AuthenticationResponse;
import com.aegis.identity.application.mapper.AuthMapper;
import com.aegis.identity.application.service.AuthenticateUserService;
import com.aegis.identity.application.service.RefreshTokenService;
import com.aegis.identity.domain.port.inbound.RefreshTokenUseCase;
import com.aegis.identity.web.dto.LoginRequest;
import com.aegis.identity.web.dto.RefreshTokenRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserService authenticateUserService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticateUserService authenticateUserService,
                          RefreshTokenService refreshTokenService) {
        this.authenticateUserService = authenticateUserService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String effectiveCorrelationId = correlationId != null
                ? correlationId
                : UUID.randomUUID().toString();

        AuthenticateUserCommand command = new AuthenticateUserCommand(
                request.email(),
                request.password(),
                effectiveCorrelationId
        );

        AuthenticationResponse response = authenticateUserService.authenticateAndReturnResponse(command);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String effectiveCorrelationId = correlationId != null
                ? correlationId
                : UUID.randomUUID().toString();

        RefreshTokenUseCase.Command command = new RefreshTokenUseCase.Command(
                request.refreshToken(),
                effectiveCorrelationId
        );

        RefreshTokenUseCase.Result result = refreshTokenService.refresh(command);

        AuthenticationResponse response = AuthMapper.toResponse(result.tokenPair(), true);

        return ResponseEntity.ok(response);
    }
}
