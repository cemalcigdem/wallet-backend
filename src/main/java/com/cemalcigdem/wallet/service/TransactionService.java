package com.cemalcigdem.wallet.service;

import com.cemalcigdem.wallet.dto.TransactionResponse;
import com.cemalcigdem.wallet.exception.AccountNotFoundException;
import com.cemalcigdem.wallet.repository.AccountRepository;
import com.cemalcigdem.wallet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public List<TransactionResponse> listByAccount(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        return transactionRepository.findLatestByAccountId(accountId)
                .stream()
                .map(t -> new TransactionResponse(
                        t.getId(),
                        t.getType(),
                        t.getStatus(),
                        t.getAmount(),
                        t.getBalanceAfter(),
                        t.getCreatedAt()
                ))
                .toList();
    }
}
