package com.cemalcigdem.wallet.service;

import com.cemalcigdem.wallet.domain.Account;
import com.cemalcigdem.wallet.domain.Transaction;
import com.cemalcigdem.wallet.domain.TransactionStatus;
import com.cemalcigdem.wallet.domain.TransactionType;
import com.cemalcigdem.wallet.dto.TransactionResponse;
import com.cemalcigdem.wallet.exception.AccountNotFoundException;
import com.cemalcigdem.wallet.repository.AccountRepository;
import com.cemalcigdem.wallet.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void statement_accountExists_returnsMappedTransactions() {

        // Arrange
        Long accountId = 10L;
        Pageable pageable = PageRequest.of(0, 2);

        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59);

        when(accountRepository.existsById(accountId)).thenReturn(true);

        Transaction transactionDeposit = createTransaction(
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                new BigDecimal("100"),
                new BigDecimal("250"),
                "REF1"
        );

        Transaction transactionFailedDeposit = createTransaction(
                TransactionType.DEPOSIT,
                TransactionStatus.FAILED,
                new BigDecimal("50"),
                new BigDecimal("250"),
                "REF2"
        );

        Page<Transaction> transactionPage =
                new PageImpl<>(List.of(transactionDeposit, transactionFailedDeposit), pageable, 2);

        when(transactionRepository.findStatement(accountId, null, from, to, pageable))
                .thenReturn(transactionPage);

        // Act
        Page<TransactionResponse> transactionResponsePage =
                transactionService.statement(accountId, null, from, to, pageable);

        // Assert
        assertEquals(2, transactionResponsePage.getContent().size());

        TransactionResponse firstTransactionResponse = transactionResponsePage.getContent().get(0);
        assertEquals(TransactionType.DEPOSIT, firstTransactionResponse.type());
        assertEquals(TransactionStatus.SUCCESS, firstTransactionResponse.status());
        assertEquals(new BigDecimal("100"), firstTransactionResponse.amount());
        assertEquals(new BigDecimal("250"), firstTransactionResponse.balanceAfter());
        assertEquals("REF1", firstTransactionResponse.referenceId());

        verify(accountRepository).existsById(accountId);
        verify(transactionRepository).findStatement(accountId, null, from, to, pageable);
    }

    @Test
    void statement_accountDoesNotExist_throwsAccountNotFoundException() {

        // Arrange
        Long accountId = 99L;
        Pageable pageable = PageRequest.of(0, 10);

        when(accountRepository.existsById(accountId)).thenReturn(false);

        // Act + Assert
        assertThrows(
                AccountNotFoundException.class,
                () -> transactionService.statement(accountId, null, null, null, pageable)
        );

        verify(accountRepository).existsById(accountId);
        verify(transactionRepository, never())
                .findStatement(any(), any(), any(), any(), any());
    }

    private Transaction createTransaction(
            TransactionType type,
            TransactionStatus status,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String referenceId
    ) {
        return new Transaction(
                mock(Account.class),
                type,
                status,
                amount,
                balanceAfter,
                referenceId,
                "IDEMPOTENCY",
                null,
                "REQ_HASH"
        );
    }
}