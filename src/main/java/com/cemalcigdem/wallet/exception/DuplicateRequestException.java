package com.cemalcigdem.wallet.exception;

public class DuplicateRequestException extends RuntimeException {
    public DuplicateRequestException(String key) {
        super("Duplicate request. Idempotency-Key=" + key);
    }
}
