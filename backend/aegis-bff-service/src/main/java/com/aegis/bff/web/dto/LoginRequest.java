package com.aegis.bff.web.dto;

/**
 * Request body for the login endpoint.
 *
 * @param email    the user's email address
 * @param password the user's password
 */
public record LoginRequest(String email, String password) {
}
