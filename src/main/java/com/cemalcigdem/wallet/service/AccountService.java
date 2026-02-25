package com.cemalcigdem.wallet.service;

import com.cemalcigdem.wallet.domain.*;
import com.cemalcigdem.wallet.dto.*;
import com.cemalcigdem.wallet.exception.*;
import com.cemalcigdem.wallet.repository.AccountRepository;
import com.cemalcigdem.wallet.repository.TransactionRepository;
import com.cemalcigdem.wallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public AccountResponse create(Long userId, AccountCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String currency = request.currency().toUpperCase();

        if (accountRepository.existsByUserAndCurrency(user, currency)) {
            throw new DuplicateAccountCurrencyException(userId, currency);
        }

        Account account = new Account(user, currency);
        Account saved = accountRepository.save(account);

        return toResponse(saved);
    }

    public List<AccountResponse> listByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return accountRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AccountResponse deposit(Long accountId, BalanceChangeRequest request, String idempotencyKey) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        String hash = hashDeposit(accountId, request.amount());

        var existingOpt = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            Transaction existing = existingOpt.get();

            if (!hash.equals(existing.getRequestHash())) {
                throw new IdempotencyConflictException(
                        idempotencyKey,
                        "Payload differs from the original request."
                );
            }

            return new AccountResponse(account.getId(), account.getCurrency(), existing.getBalanceAfter(), account.getCreatedAt());
        }

        account.increaseBalance(request.amount());

        Transaction tx = new Transaction(
                account,
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                request.amount(),
                account.getBalance(),
                null,
                idempotencyKey,
                null,
                hash
        );

        try {
            transactionRepository.save(tx);
            return toResponse(account);
        } catch (DataIntegrityViolationException e) {
            Transaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();

            return new AccountResponse(
                    account.getId(),
                    account.getCurrency(),
                    existing.getBalanceAfter(),
                    account.getCreatedAt()
            );
        }
    }

    @Transactional
    public AccountResponse withdraw(Long accountId, BalanceChangeRequest request, String idempotencyKey) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        String hash = hashWithdraw(accountId, request.amount());

        var existingOpt = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            Transaction existing = existingOpt.get();

            if (!hash.equals(existing.getRequestHash())) {
                throw new IdempotencyConflictException(
                        idempotencyKey,
                        "Payload differs from the original request."
                );
            }

            return new AccountResponse(account.getId(), account.getCurrency(), existing.getBalanceAfter(), account.getCreatedAt());
        }

        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException(accountId, account.getBalance(), request.amount());
        }

        account.decreaseBalance(request.amount());

        Transaction tx = new Transaction(
                account,
                TransactionType.WITHDRAW,
                TransactionStatus.SUCCESS,
                request.amount(),
                account.getBalance(),
                null,
                idempotencyKey,
                null,
                hash
        );

        try {
            transactionRepository.save(tx);
            return toResponse(account);
        } catch (DataIntegrityViolationException e) {
            Transaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();

            return new AccountResponse(
                    account.getId(),
                    account.getCurrency(),
                    existing.getBalanceAfter(),
                    account.getCreatedAt()
            );
        }
    }

    @Transactional
    public TransferResponse transfer(Long fromAccountId, TransferRequest request, String idempotencyKey) {
        String requestHash = hashTransfer(
                fromAccountId,
                request.toAccountId(),
                request.amount()
        );

        return transactionRepository
                .findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT)
                .map(existingTx -> {
                    if (!requestHash.equals(existingTx.getRequestHash())) {
                        throw new IdempotencyConflictException(
                                idempotencyKey,
                                "Transfer payload differs from the original request"
                        );
                    }
                    return toTransferResponseFromTransaction(existingTx);
                })
                .orElseGet(() -> doTransfer(fromAccountId, request, idempotencyKey, requestHash));
    }

    private TransferResponse doTransfer(Long fromAccountId, TransferRequest request, String idempotencyKey, String requestHash) {
        Long toAccountId = request.toAccountId();

        if (fromAccountId.equals(toAccountId)) {
            throw new SameAccountTransferException(fromAccountId);
        }

        Account from = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException(fromAccountId));

        Account to = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new AccountNotFoundException(toAccountId));

        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new CurrencyMismatchException(fromAccountId, toAccountId);
        }

        BigDecimal amount = request.amount();
        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(fromAccountId, from.getBalance(), amount);
        }

        from.decreaseBalance(amount);
        to.increaseBalance(amount);

        String transferRef = UUID.randomUUID().toString();

        Transaction outTx = new Transaction(
                from,
                TransactionType.TRANSFER_OUT,
                TransactionStatus.SUCCESS,
                amount,
                from.getBalance(),
                transferRef,
                idempotencyKey,
                to.getId(),
                requestHash
        );

        Transaction inTx = new Transaction(
                to,
                TransactionType.TRANSFER_IN,
                TransactionStatus.SUCCESS,
                amount,
                to.getBalance(),
                transferRef,
                null,
                from.getId(),
                null
        );

        try {
            transactionRepository.save(outTx);
            transactionRepository.save(inTx);

            return new TransferResponse(transferRef, from.getId(), to.getId(), amount);
        } catch (DataIntegrityViolationException e) {
            Transaction existing = transactionRepository
                    .findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT)
                    .orElseThrow();

            return toTransferResponseFromTransaction(existing);
        }
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCurrency(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }

    private TransferResponse toTransferResponseFromTransaction(Transaction tx) {
        String transferRef = tx.getReferenceId();
        BigDecimal amount = tx.getAmount();

        Long accountId = tx.getAccount().getId();
        Long counterparty = tx.getCounterpartyAccountId();

        return new TransferResponse(transferRef, accountId, counterparty, amount);
    }

    private String normalizeAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute request hash", e);
        }
    }

    private String hashDeposit(Long accountId, BigDecimal amount) {
        String canonical = "DEPOSIT|accountId=" + accountId + "|amount=" + normalizeAmount(amount);
        return sha256Hex(canonical);
    }

    private String hashWithdraw(Long accountId, BigDecimal amount) {
        String canonical = "WITHDRAW|accountId=" + accountId + "|amount=" + normalizeAmount(amount);
        return sha256Hex(canonical);
    }

    private String hashTransfer(Long fromId, Long toId, BigDecimal amount) {
        String canonical = "TRANSFER|from=" + fromId + "|to=" + toId + "|amount=" + normalizeAmount(amount);
        return sha256Hex(canonical);
    }
}