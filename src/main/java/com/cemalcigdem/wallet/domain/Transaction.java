package com.cemalcigdem.wallet.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "account_id",
            foreignKey = @ForeignKey(name = "fk_transaction_account")
    )
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(length = 36)
    private String referenceId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 64, unique = true)
    private String idempotencyKey;

    @Column(name = "counterparty_account_id")
    private Long counterpartyAccountId;

    @Column(length = 64)
    private String requestHash;

    protected Transaction() {
    }

    public Transaction(
            Account account,
            TransactionType type,
            TransactionStatus status,
            BigDecimal amount,
            BigDecimal balanceAfter
    ) {
        this(account, type, status, amount, balanceAfter, null, null, null, null);
    }

    public Transaction(Account account,
                       TransactionType type,
                       TransactionStatus status,
                       BigDecimal amount,
                       BigDecimal balanceAfter,
                       String referenceId) {
        this(account, type, status, amount, balanceAfter, referenceId, null, null, null);
    }

    public Transaction(
            Account account,
            TransactionType type,
            TransactionStatus status,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String referenceId,
            String idempotencyKey) {
        this(account, type, status, amount, balanceAfter, referenceId, idempotencyKey, null, null);
    }

    public Transaction(
            Account account,
            TransactionType type,
            TransactionStatus status,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String referenceId,
            String idempotencyKey,
            Long counterpartyAccountId,
            String requestHash
    ) {
        this.account = account;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceId = referenceId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = LocalDateTime.now();
        this.counterpartyAccountId = counterpartyAccountId;
        this.requestHash = requestHash;
    }

}