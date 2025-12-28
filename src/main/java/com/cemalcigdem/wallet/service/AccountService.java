package com.cemalcigdem.wallet.service;

import com.cemalcigdem.wallet.domain.Account;
import com.cemalcigdem.wallet.domain.User;
import com.cemalcigdem.wallet.dto.AccountCreateRequest;
import com.cemalcigdem.wallet.dto.AccountResponse;
import com.cemalcigdem.wallet.exception.DuplicateAccountCurrencyException;
import com.cemalcigdem.wallet.exception.UserNotFoundException;
import com.cemalcigdem.wallet.repository.AccountRepository;
import com.cemalcigdem.wallet.repository.UserRepository;
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

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCurrency(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }
}