package com.cemalcigdem.wallet.service;

import com.cemalcigdem.wallet.domain.TransactionType;
import com.cemalcigdem.wallet.dto.TransactionResponse;
import com.cemalcigdem.wallet.exception.AccountNotFoundException;
import com.cemalcigdem.wallet.repository.AccountRepository;
import com.cemalcigdem.wallet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public Page<TransactionResponse> statement(
            Long accountId,
            TransactionType type,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        return transactionRepository.findStatement(accountId, type, from, to, pageable)
                .map(t -> new TransactionResponse(
                        t.getId(),
                        t.getType(),
                        t.getStatus(),
                        t.getAmount(),
                        t.getBalanceAfter(),
                        t.getReferenceId(),
                        t.getCreatedAt()
                ));
    }
}
