package com.aegis.identity.web.controller;

import com.aegis.identity.application.dto.RegisterUserCommand;
import com.aegis.identity.application.dto.UserRegistrationResponse;
import com.aegis.identity.application.service.RegisterUserService;
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

    private final RegisterUserService registerUserService;

    public RegistrationController(RegisterUserService registerUserService) {
        this.registerUserService = registerUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> register(
            @Valid @RequestBody RegisterUserRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String effectiveCorrelationId = correlationId != null
                ? correlationId
                : UUID.randomUUID().toString();

        RegisterUserCommand command = new RegisterUserCommand(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                effectiveCorrelationId
        );

        UserRegistrationResponse response = registerUserService.registerAndReturnResponse(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
