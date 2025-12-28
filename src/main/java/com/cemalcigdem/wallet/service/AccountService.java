package com.cemalcigdem.wallet.service;

import com.cemalcigdem.wallet.domain.Account;
import com.cemalcigdem.wallet.domain.User;
import com.cemalcigdem.wallet.dto.AccountCreateRequest;
import com.cemalcigdem.wallet.dto.AccountResponse;
import com.cemalcigdem.wallet.dto.BalanceChangeRequest;
import com.cemalcigdem.wallet.exception.AccountNotFoundException;
import com.cemalcigdem.wallet.exception.DuplicateAccountCurrencyException;
import com.cemalcigdem.wallet.exception.InsufficientBalanceException;
import com.cemalcigdem.wallet.exception.UserNotFoundException;
import com.cemalcigdem.wallet.repository.AccountRepository;
import com.cemalcigdem.wallet.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

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
        return toResponse(account);
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