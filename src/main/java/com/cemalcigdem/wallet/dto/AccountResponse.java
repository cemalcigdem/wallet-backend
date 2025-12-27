package com.cemalcigdem.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountResponse {

    private final Long id;
    private final String currency;
    private final BigDecimal balance;
    private final LocalDateTime createdAt;

    public AccountResponse(Long id, String currency, BigDecimal balance, LocalDateTime createdAt) {
        this.id = id;
        this.currency = currency;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}