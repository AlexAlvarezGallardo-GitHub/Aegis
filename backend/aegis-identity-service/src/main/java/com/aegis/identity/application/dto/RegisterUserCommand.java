package com.aegis.identity.application.dto;

public record RegisterUserCommand(
        String email,
        String password,
        String firstName,
        String lastName,
        String correlationId
) {
}
