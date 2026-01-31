package com.cemalcigdem.wallet.exception;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(Long fromAccountId, Long toAccountId) {
        super("Currency mismatch between accounts: fromAccountId=" + fromAccountId + " toAccountId=" + toAccountId);
    }
}
