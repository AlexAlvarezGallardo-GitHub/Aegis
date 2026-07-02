package com.aegis.identity.application.dto;

public record AuthenticateUserCommand(
        String email,
        String password,
        String correlationId
) {
}
