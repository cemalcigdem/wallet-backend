package com.cemalcigdem.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(Long id, String currency, BigDecimal balance, LocalDateTime createdAt) {
}