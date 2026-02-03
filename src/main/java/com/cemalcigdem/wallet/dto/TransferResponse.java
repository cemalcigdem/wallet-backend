package com.cemalcigdem.wallet.dto;

import java.math.BigDecimal;

public record TransferResponse(
        String transferRef,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount
) {
}
