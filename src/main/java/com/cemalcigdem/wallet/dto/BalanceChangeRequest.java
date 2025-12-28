package com.cemalcigdem.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BalanceChangeRequest(
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be >= 0.01")
        BigDecimal amount
) {
}
