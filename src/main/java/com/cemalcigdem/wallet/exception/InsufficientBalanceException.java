package com.cemalcigdem.wallet.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(Long accountId, BigDecimal balance, BigDecimal amount) {
        super("Insufficient balance for accountId=" + accountId +
                " balance=" + balance + " amount=" + amount);
    }
}
