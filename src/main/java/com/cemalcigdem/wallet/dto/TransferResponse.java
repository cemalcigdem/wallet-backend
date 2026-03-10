package com.cemalcigdem.wallet.dto;

import java.math.BigDecimal;

public record TransferResponse(
        String transferReference,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount
) {
}
