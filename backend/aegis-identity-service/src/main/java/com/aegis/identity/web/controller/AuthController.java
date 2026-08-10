package com.aegis.identity.web.controller;

import com.aegis.identity.application.dto.AuthenticationResponse;
import com.aegis.identity.domain.port.inbound.AuthenticateUserUseCase;
import com.aegis.identity.domain.port.inbound.RefreshTokenUseCase;
import com.aegis.identity.domain.port.outbound.TokenProvider;
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

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final TokenProvider tokenProvider;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          TokenProvider tokenProvider) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String effectiveCorrelationId = correlationId != null
                ? correlationId
                : UUID.randomUUID().toString();

        AuthenticateUserUseCase.Command command = new AuthenticateUserUseCase.Command(
                request.email(),
                request.password(),
                effectiveCorrelationId
        );

        AuthenticateUserUseCase.Result result = authenticateUserUseCase.authenticate(command);

        AuthenticationResponse response = AuthenticationResponse.of(
                result.tokenPair(),
                result.emailVerified(),
                tokenProvider.getAccessTokenExpirySeconds()
        );

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

        RefreshTokenUseCase.Result result = refreshTokenUseCase.refresh(command);

        AuthenticationResponse response = AuthenticationResponse.of(
                result.tokenPair(),
                true,
                tokenProvider.getAccessTokenExpirySeconds()
        );

        return ResponseEntity.ok(response);
    }
}
