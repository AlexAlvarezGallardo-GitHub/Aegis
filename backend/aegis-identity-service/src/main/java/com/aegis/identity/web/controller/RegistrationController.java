package com.aegis.identity.web.controller;

import com.aegis.identity.application.dto.UserRegistrationResponse;
import com.aegis.identity.domain.port.inbound.RegisterUserUseCase;
import com.aegis.identity.web.dto.RegisterUserRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class RegistrationController {

    private final RegisterUserUseCase registerUserUseCase;

    public RegistrationController(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> register(
            @Valid @RequestBody RegisterUserRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String effectiveCorrelationId = correlationId != null
                ? correlationId
                : UUID.randomUUID().toString();

        RegisterUserUseCase.Command command = new RegisterUserUseCase.Command(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                effectiveCorrelationId
        );

        RegisterUserUseCase.Result result = registerUserUseCase.register(command);

        UserRegistrationResponse response = new UserRegistrationResponse(
                result.userId(),
                result.email(),
                result.status(),
                result.registeredAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
