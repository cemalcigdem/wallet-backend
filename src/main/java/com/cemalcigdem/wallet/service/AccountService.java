package com.cemalcigdem.wallet.service;

import com.cemalcigdem.wallet.domain.*;
import com.cemalcigdem.wallet.dto.AccountCreateRequest;
import com.cemalcigdem.wallet.dto.AccountResponse;
import com.cemalcigdem.wallet.dto.BalanceChangeRequest;
import com.cemalcigdem.wallet.dto.TransferRequest;
import com.cemalcigdem.wallet.exception.*;
import com.cemalcigdem.wallet.repository.AccountRepository;
import com.cemalcigdem.wallet.repository.TransactionRepository;
import com.cemalcigdem.wallet.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    public AccountResponse deposit(Long accountId, BalanceChangeRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        account.increaseBalance(request.amount());

        Transaction tx = new Transaction(
                account,
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                request.amount(),
                account.getBalance()
        );
        transactionRepository.save(tx);

        return toResponse(account);
    }

    @Transactional
    public AccountResponse withdraw(Long accountId, BalanceChangeRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException(accountId, account.getBalance(), request.amount());
        }

        account.decreaseBalance(request.amount());


        Transaction tx = new Transaction(
                account,
                TransactionType.WITHDRAW,
                TransactionStatus.SUCCESS,
                request.amount(),
                account.getBalance()
        );
        transactionRepository.save(tx);

        return toResponse(account);
    }

    @Transactional
    public void transfer(Long fromAccountId, TransferRequest request) {
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
                from.getBalance()
        );

        Transaction inTx = new Transaction(
                to,
                TransactionType.TRANSFER_IN,
                TransactionStatus.SUCCESS,
                amount,
                to.getBalance()
        );

        transactionRepository.save(outTx);
        transactionRepository.save(inTx);
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCurrency(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }
}