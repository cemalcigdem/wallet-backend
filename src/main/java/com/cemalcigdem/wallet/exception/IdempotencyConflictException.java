package com.cemalcigdem.wallet.exception;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String idempotencyKey, String details) {
        super("Idempotency-Key conflict for key=" + idempotencyKey + ". " + details);
    }
}