package com.cemalcigdem.wallet.dto;

import com.cemalcigdem.wallet.domain.TransactionStatus;
import com.cemalcigdem.wallet.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        BigDecimal balanceAfter,
        LocalDateTime createdAt
) {
}
