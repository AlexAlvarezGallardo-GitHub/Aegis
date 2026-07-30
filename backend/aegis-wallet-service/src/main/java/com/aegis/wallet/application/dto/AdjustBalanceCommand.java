package com.aegis.wallet.application.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdjustBalanceCommand(
        @NotNull(message = "Amount is required")
        BigDecimal amount,
        String description
) {}
