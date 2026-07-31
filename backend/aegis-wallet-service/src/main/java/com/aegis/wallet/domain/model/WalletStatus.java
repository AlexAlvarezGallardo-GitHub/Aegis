package com.aegis.wallet.domain.model;

import com.aegis.wallet.domain.exception.WalletOperationNotAllowedException;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum WalletStatus {
    ACTIVE,
    FROZEN,
    CLOSED;

    /**
     * Parses a status string into a {@code WalletStatus}, providing a descriptive
     * error message if the value is invalid.
     *
     * @param value the status string to parse
     * @return the matching WalletStatus
     * @throws WalletOperationNotAllowedException if the value does not match any status
     */
    public static WalletStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new WalletOperationNotAllowedException(
                    "Status must not be blank. Valid values: " + validValues());
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WalletOperationNotAllowedException(
                    "Invalid status: '" + value + "'. Valid values: " + validValues());
        }
    }

    private static String validValues() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
