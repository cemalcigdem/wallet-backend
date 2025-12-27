package com.cemalcigdem.wallet.exception;

public class DuplicateAccountCurrencyException extends RuntimeException {
    public DuplicateAccountCurrencyException(Long userId, String currency) {
        super("Account already exists for userId=" + userId + " currency=" + currency);
    }
}