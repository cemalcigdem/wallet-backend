package com.cemalcigdem.wallet.service;

import com.cemalcigdem.wallet.domain.*;
import com.cemalcigdem.wallet.dto.*;
import com.cemalcigdem.wallet.exception.*;
import com.cemalcigdem.wallet.repository.AccountRepository;
import com.cemalcigdem.wallet.repository.TransactionRepository;
import com.cemalcigdem.wallet.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void create_userExistsAndCurrencyIsNew_returnsSavedAccountResponse() {
        // Arrange
        Long userId = 1L;
        User user = createUser("Test User", "test@example.com");
        AccountCreateRequest accountCreateRequest = new AccountCreateRequest("try");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.existsByUserAndCurrency(user, "TRY")).thenReturn(false);
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccountResponse accountResponse = accountService.create(userId, accountCreateRequest);

        // Assert
        assertNotNull(accountResponse);
        assertEquals("TRY", accountResponse.currency());
        assertEquals(BigDecimal.ZERO, accountResponse.balance());

        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);
        verify(userRepository).findById(userId);
        verify(accountRepository).existsByUserAndCurrency(user, "TRY");
        verify(accountRepository).save(accountArgumentCaptor.capture());

        Account accountToSave = accountArgumentCaptor.getValue();
        assertEquals("TRY", accountToSave.getCurrency());
        assertEquals(BigDecimal.ZERO, accountToSave.getBalance());

        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void create_userDoesNotExist_throwsUserNotFoundException() {
        // Arrange
        Long userId = 99L;
        AccountCreateRequest accountCreateRequest = new AccountCreateRequest("try");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                UserNotFoundException.class,
                () -> accountService.create(userId, accountCreateRequest)
        );

        verify(userRepository).findById(userId);
        verify(accountRepository, never()).existsByUserAndCurrency(any(), any());
        verify(accountRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void create_currencyAlreadyExistsForUser_throwsDuplicateAccountCurrencyException() {
        // Arrange
        Long userId = 1L;
        User user = createUser("Test User", "test@example.com");
        AccountCreateRequest accountCreateRequest = new AccountCreateRequest("usd");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.existsByUserAndCurrency(user, "USD")).thenReturn(true);

        // Act + Assert
        assertThrows(
                DuplicateAccountCurrencyException.class,
                () -> accountService.create(userId, accountCreateRequest)
        );

        verify(userRepository).findById(userId);
        verify(accountRepository).existsByUserAndCurrency(user, "USD");
        verify(accountRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void listByUser_userExists_returnsMappedAccountResponses() {
        // Arrange
        Long userId = 1L;
        User user = createUser("Test User", "test@example.com");

        Account tryAccount = createAccount(user, "TRY", "150.00");
        Account usdAccount = createAccount(user, "USD", "25.00");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.findByUser(user)).thenReturn(List.of(tryAccount, usdAccount));

        // Act
        List<AccountResponse> accountResponses = accountService.listByUser(userId);

        // Assert
        assertEquals(2, accountResponses.size());

        AccountResponse firstAccountResponse = accountResponses.get(0);
        assertEquals("TRY", firstAccountResponse.currency());
        assertEquals(new BigDecimal("150.00"), firstAccountResponse.balance());

        AccountResponse secondAccountResponse = accountResponses.get(1);
        assertEquals("USD", secondAccountResponse.currency());
        assertEquals(new BigDecimal("25.00"), secondAccountResponse.balance());

        verify(userRepository).findById(userId);
        verify(accountRepository).findByUser(user);
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void listByUser_userDoesNotExist_throwsUserNotFoundException() {
        // Arrange
        Long userId = 99L;
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                UserNotFoundException.class,
                () -> accountService.listByUser(99L)
        );
    }

    @Test
    void deposit_accountDoesNotExist_throwsAccountNotFoundException() {
        // Arrange
        Long accountId = 99L;
        String idempotencyKey = "idem-deposit-1";
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest(new BigDecimal("40.00"));
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.deposit(accountId, balanceChangeRequest, idempotencyKey)
        );
    }

    @Test
    void deposit_noExistingIdempotentTransaction_increasesBalanceAndSavesTransaction() {
        // Arrange
        Long accountId = 10L;
        String idempotencyKey = "idem-deposit-1";
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest(new BigDecimal("40.00"));
        Account account = createAccount(createUser("Test User", "test@example.com"), "TRY", "100.00");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.DEPOSIT))
                .thenReturn(Optional.empty());

        // Act
        AccountResponse accountResponse = accountService.deposit(accountId, balanceChangeRequest, idempotencyKey);

        // Assert
        assertNotNull(accountResponse);
        assertEquals("TRY", accountResponse.currency());
        assertEquals(new BigDecimal("140.00"), accountResponse.balance());
        assertEquals(new BigDecimal("140.00"), account.getBalance());

        verify(accountRepository).findById(accountId);
        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.DEPOSIT);
        verify(transactionRepository).save(any(Transaction.class));
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void deposit_existingIdempotentTransactionWithSamePayload_returnsExistingBalanceWithoutSavingNewTransaction() {
        // Arrange
        Long accountId = 10L;
        String idempotencyKey = "idem-deposit-1";
        BigDecimal amount = new BigDecimal("40.00");

        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest(amount);
        Account account = createAccount(createUser("Test User", "test@example.com"), "TRY", "100.00");

        String requestHash = hashDeposit(accountId, amount);
        Transaction existingTransaction = createTransaction(
                account,
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                amount,
                new BigDecimal("140.00"),
                null,
                idempotencyKey,
                null,
                requestHash
        );

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.DEPOSIT))
                .thenReturn(Optional.of(existingTransaction));

        // Act
        AccountResponse accountResponse = accountService.deposit(accountId, balanceChangeRequest, idempotencyKey);

        // Assert
        assertNotNull(accountResponse);
        assertEquals("TRY", accountResponse.currency());
        assertEquals(new BigDecimal("140.00"), accountResponse.balance());

        verify(accountRepository).findById(accountId);
        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.DEPOSIT);
        verify(transactionRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void deposit_existingIdempotentTransactionWithDifferentPayload_throwsIdempotencyConflictException() {
        // Arrange
        Long accountId = 10L;
        String idempotencyKey = "idem-deposit-1";

        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest(new BigDecimal("40.00"));
        Account account = createAccount(createUser("Test User", "test@example.com"), "TRY", "100.00");

        Transaction existingTransaction = createTransaction(
                account,
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                new BigDecimal("40.00"),
                new BigDecimal("140.00"),
                null,
                idempotencyKey,
                null,
                "different-hash"
        );

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.DEPOSIT))
                .thenReturn(Optional.of(existingTransaction));

        // Act + Assert
        assertThrows(
                IdempotencyConflictException.class,
                () -> accountService.deposit(accountId, balanceChangeRequest, idempotencyKey)
        );

        verify(accountRepository).findById(accountId);
        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.DEPOSIT);
        verify(transactionRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void deposit_transactionSaveThrowsDataIntegrityViolation_returnsExistingTransactionBalance() {
        // Arrange
        Long accountId = 10L;
        String idempotencyKey = "idem-deposit-1";
        BigDecimal amount = new BigDecimal("40.00");

        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest(amount);
        Account account = createAccount(createUser("Test User", "test@example.com"), "TRY", "100.00");

        String requestHash = hashDeposit(accountId, amount);
        Transaction existingTransaction = createTransaction(
                account,
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                amount,
                new BigDecimal("140.00"),
                null,
                idempotencyKey,
                null,
                requestHash
        );

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.DEPOSIT))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingTransaction));
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(transactionRepository).save(any(Transaction.class));

        // Act
        AccountResponse accountResponse = accountService.deposit(accountId, balanceChangeRequest, idempotencyKey);

        // Assert
        assertNotNull(accountResponse);
        assertEquals("TRY", accountResponse.currency());
        assertEquals(new BigDecimal("140.00"), accountResponse.balance());

        verify(accountRepository).findById(accountId);
        verify(transactionRepository, times(2))
                .findByIdempotencyKeyAndType(idempotencyKey, TransactionType.DEPOSIT);
        verify(transactionRepository).save(any(Transaction.class));
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void deposit_messageDigestFails_throwsIllegalStateException() {
        // Arrange
        Long accountId = 10L;
        String idempotencyKey = "idem-deposit-hash-fail";
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest(new BigDecimal("40.00"));
        Account account = createAccount(createUser("Test User", "test@example.com"), "TRY", "100.00");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        try (MockedStatic<MessageDigest> messageDigestMockedStatic = mockStatic(MessageDigest.class)) {
            messageDigestMockedStatic.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("boom"));

            // Act + Assert
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> accountService.deposit(accountId, balanceChangeRequest, idempotencyKey)
            );

            assertEquals("Failed to compute request hash", exception.getMessage());
        }

        verify(accountRepository).findById(accountId);
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void withdraw_existingIdempotentTransactionWithSamePayload_returnsExistingWithdrawResponse() {
        // Arrange
        Long accountId = 20L;
        String idempotencyKey = "idem-withdraw-1";
        BigDecimal amount = new BigDecimal("30.00");

        Account account = createAccount(createUser("Test User", "test@example.com"), "TRY", "100.00");

        String requestHash = hashWithdraw(accountId, amount);
        Transaction existingTransaction = createTransaction(
                account,
                TransactionType.WITHDRAW,
                TransactionStatus.SUCCESS,
                amount,
                new BigDecimal("140.00"),
                null,
                idempotencyKey,
                null,
                requestHash
        );

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.WITHDRAW))
                .thenReturn(Optional.of(existingTransaction));

        // Act
        AccountResponse accountResponse = accountService.withdraw(accountId, new BalanceChangeRequest(amount), idempotencyKey);

        // Assert
        assertNotNull(accountResponse);
        assertEquals("TRY", accountResponse.currency());
        assertEquals(new BigDecimal("140.00"), accountResponse.balance());

        verify(accountRepository).findById(accountId);
        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.WITHDRAW);
        verify(transactionRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void withdraw_existingIdempotentTransactionWithDifferentPayload_throwsIdempotencyConflictException() {
        // Arrange
        Long accountId = 10L;
        String idempotencyKey = "idem-deposit-1";

        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest(new BigDecimal("40.00"));
        Account account = createAccount(createUser("Test User", "test@example.com"), "TRY", "100.00");

        Transaction existingTransaction = createTransaction(
                account,
                TransactionType.WITHDRAW,
                TransactionStatus.FAILED,
                new BigDecimal("40.00"),
                new BigDecimal("140.00"),
                null,
                idempotencyKey,
                null,
                "different-hash"
        );

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.WITHDRAW))
                .thenReturn(Optional.of(existingTransaction));

        // Act + Assert
        assertThrows(
                IdempotencyConflictException.class,
                () -> accountService.withdraw(accountId, balanceChangeRequest, idempotencyKey)
        );

        verify(accountRepository).findById(accountId);
        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.WITHDRAW);
        verify(transactionRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void withdraw_sufficientBalanceEqualToRequestBalance_decreasesBalanceAndSavesTransaction() {
        // Arrange
        Long accountId = 20L;
        String idempotencyKey = "idem-withdraw-1";
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest(new BigDecimal("100.00"));
        Account account = createAccount(createUser("Test User", "test@example.com"), "TRY", "100.00");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.WITHDRAW))
                .thenReturn(Optional.empty());

        // Act
        AccountResponse accountResponse = accountService.withdraw(accountId, balanceChangeRequest, idempotencyKey);

        // Assert
        assertNotNull(accountResponse);
        assertEquals("TRY", accountResponse.currency());
        assertEquals(new BigDecimal("0.00"), accountResponse.balance());
        assertEquals(new BigDecimal("0.00"), account.getBalance());

        verify(accountRepository).findById(accountId);
        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.WITHDRAW);
        verify(transactionRepository).save(any(Transaction.class));
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void withdraw_sufficientBalanceMoreThanRequestBalance_decreasesBalanceAndSavesTransaction() {
        // Arrange
        Long accountId = 20L;
        String idempotencyKey = "idem-withdraw-1";
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest(new BigDecimal("30.00"));
        Account account = createAccount(createUser("Test User", "test@example.com"), "TRY", "100.00");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.WITHDRAW))
                .thenReturn(Optional.empty());

        // Act
        AccountResponse accountResponse = accountService.withdraw(accountId, balanceChangeRequest, idempotencyKey);

        // Assert
        assertNotNull(accountResponse);
        assertEquals("TRY", accountResponse.currency());
        assertEquals(new BigDecimal("70.00"), accountResponse.balance());
        assertEquals(new BigDecimal("70.00"), account.getBalance());

        verify(accountRepository).findById(accountId);
        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.WITHDRAW);
        verify(transactionRepository).save(any(Transaction.class));
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void withdraw_accountDoesNotExist_throwsAccountNotFoundException() {
        // Arrange
        Long accountId = 99L;
        String idempotencyKey = "idem-withdraw-1";
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest(new BigDecimal("30.00"));
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.withdraw(accountId, balanceChangeRequest, idempotencyKey)
        );
    }

    @Test
    void withdraw_insufficientBalance_throwsInsufficientBalanceException() {
        // Arrange
        Long accountId = 20L;
        String idempotencyKey = "idem-withdraw-1";
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest(new BigDecimal("70.00"));
        Account account = createAccount(createUser("Test User", "test@example.com"), "TRY", "50.00");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.WITHDRAW))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                InsufficientBalanceException.class,
                () -> accountService.withdraw(accountId, balanceChangeRequest, idempotencyKey)
        );

        verify(accountRepository).findById(accountId);
        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.WITHDRAW);
        verify(transactionRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void withdraw_transactionSaveThrowsDataIntegrityViolation_returnsExistingTransactionBalance() {
        // Arrange
        Long accountId = 10L;
        String idempotencyKey = "idem-deposit-1";
        BigDecimal amount = new BigDecimal("40.00");

        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest(amount);
        Account account = createAccount(createUser("Test User", "test@example.com"), "TRY", "100.00");

        String requestHash = hashDeposit(accountId, amount);
        Transaction existingTransaction = createTransaction(
                account,
                TransactionType.WITHDRAW,
                TransactionStatus.SUCCESS,
                amount,
                new BigDecimal("140.00"),
                null,
                idempotencyKey,
                null,
                requestHash
        );

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.WITHDRAW))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingTransaction));
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(transactionRepository).save(any(Transaction.class));

        // Act
        AccountResponse accountResponse = accountService.withdraw(accountId, balanceChangeRequest, idempotencyKey);

        // Assert
        assertNotNull(accountResponse);
        assertEquals("TRY", accountResponse.currency());
        assertEquals(new BigDecimal("140.00"), accountResponse.balance());

        verify(accountRepository).findById(accountId);
        verify(transactionRepository, times(2))
                .findByIdempotencyKeyAndType(idempotencyKey, TransactionType.WITHDRAW);
        verify(transactionRepository).save(any(Transaction.class));
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void transfer_noExistingIdempotentTransactionRequestIsLessThanBalance_transfersMoneyAndSavesTwoTransactions() {
        // Arrange
        Long fromAccountId = 100L;
        Long toAccountId = 200L;
        String idempotencyKey = "idem-transfer-1";

        TransferRequest transferRequest = new TransferRequest(toAccountId, new BigDecimal("30.00"));

        Account fromAccount = createAccount(createUser("From User", "from@example.com"), "TRY", "200.00");
        Account toAccount = createAccount(createUser("To User", "to@example.com"), "TRY", "50.00");

        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

        // Act
        TransferResponse transferResponse = accountService.transfer(fromAccountId, transferRequest, idempotencyKey);

        // Assert
        assertNotNull(transferResponse);
        assertNotNull(transferResponse.transferReference());
        assertEquals(new BigDecimal("30.00"), transferResponse.amount());

        assertEquals(new BigDecimal("170.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("80.00"), toAccount.getBalance());

        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT);
        verify(accountRepository).findById(fromAccountId);
        verify(accountRepository).findById(toAccountId);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void transfer_noExistingIdempotentTransactionRequestIsEqualToBalance_transfersMoneyAndSavesTwoTransactions() {
        // Arrange
        Long fromAccountId = 100L;
        Long toAccountId = 200L;
        String idempotencyKey = "idem-transfer-1";

        TransferRequest transferRequest = new TransferRequest(toAccountId, new BigDecimal("200.00"));

        Account fromAccount = createAccount(createUser("From User", "from@example.com"), "TRY", "200.00");
        Account toAccount = createAccount(createUser("To User", "to@example.com"), "TRY", "50.00");

        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

        // Act
        TransferResponse transferResponse = accountService.transfer(fromAccountId, transferRequest, idempotencyKey);

        // Assert
        assertNotNull(transferResponse);
        assertNotNull(transferResponse.transferReference());
        assertEquals(new BigDecimal("200.00"), transferResponse.amount());

        assertEquals(new BigDecimal("0.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("250.00"), toAccount.getBalance());

        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT);
        verify(accountRepository).findById(fromAccountId);
        verify(accountRepository).findById(toAccountId);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void transfer_existingIdempotentTransactionWithSamePayload_returnsExistingTransferResponse() {
        // Arrange
        Long fromAccountId = 100L;
        Long toAccountId = 200L;
        String idempotencyKey = "idem-transfer-1";
        BigDecimal amount = new BigDecimal("30.00");

        TransferRequest transferRequest = new TransferRequest(toAccountId, amount);
        Account fromAccount = createAccount(createUser("From User", "from@example.com"), "TRY", "200.00");

        String requestHash = hashTransfer(fromAccountId, toAccountId, amount);
        Transaction existingTransaction = createTransaction(
                fromAccount,
                TransactionType.TRANSFER_OUT,
                TransactionStatus.SUCCESS,
                amount,
                new BigDecimal("170.00"),
                "transfer-ref-1",
                idempotencyKey,
                toAccountId,
                requestHash
        );

        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT))
                .thenReturn(Optional.of(existingTransaction));

        // Act
        TransferResponse transferResponse = accountService.transfer(fromAccountId, transferRequest, idempotencyKey);

        // Assert
        assertNotNull(transferResponse);
        assertEquals("transfer-ref-1", transferResponse.transferReference());
        assertEquals(amount, transferResponse.amount());
        assertEquals(toAccountId, transferResponse.toAccountId());

        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT);
        verify(accountRepository, never()).findById(anyLong());
        verify(transactionRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void transfer_sameSourceAndTargetAccount_throwsSameAccountTransferException() {
        // Arrange
        Long fromAccountId = 100L;
        String idempotencyKey = "idem-transfer-1";
        TransferRequest transferRequest = new TransferRequest(fromAccountId, new BigDecimal("30.00"));

        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                SameAccountTransferException.class,
                () -> accountService.transfer(fromAccountId, transferRequest, idempotencyKey)
        );

        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT);
        verify(accountRepository, never()).findById(anyLong());
        verify(transactionRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void transfer_currencyMismatch_throwsCurrencyMismatchException() {
        // Arrange
        Long fromAccountId = 100L;
        Long toAccountId = 200L;
        String idempotencyKey = "idem-transfer-1";

        TransferRequest transferRequest = new TransferRequest(toAccountId, new BigDecimal("30.00"));

        Account fromAccount = createAccount(createUser("From User", "from@example.com"), "TRY", "200.00");
        Account toAccount = createAccount(createUser("To User", "to@example.com"), "USD", "50.00");

        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

        // Act + Assert
        assertThrows(
                CurrencyMismatchException.class,
                () -> accountService.transfer(fromAccountId, transferRequest, idempotencyKey)
        );

        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT);
        verify(accountRepository).findById(fromAccountId);
        verify(accountRepository).findById(toAccountId);
        verify(transactionRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void transfer_fromAccountDoesNotExist_throwsAccountNotFoundException() {
        // Arrange
        Long fromAccountId = 99L;
        Long toAccountId = 200L;
        String idempotencyKey = "idem-transfer-1";
        TransferRequest transferRequest = new TransferRequest(toAccountId, new BigDecimal("30.00"));

        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.transfer(fromAccountId, transferRequest, idempotencyKey)
        );

        verify(accountRepository).findById(fromAccountId);
        verify(accountRepository, never()).findById(toAccountId);
        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT);
        verify(transactionRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void transfer_toAccountDoesNotExist_throwsAccountNotFoundException() {
        // Arrange
        Long fromAccountId = 99L;
        Long toAccountId = 200L;
        String idempotencyKey = "idem-transfer-1";
        TransferRequest transferRequest = new TransferRequest(toAccountId, new BigDecimal("30.00"));
        Account account = createAccount(createUser("TestUser", "test@example.com"), "TRY", "100.00");

        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(account));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.transfer(fromAccountId, transferRequest, idempotencyKey)
        );

        verify(accountRepository).findById(fromAccountId);
        verify(accountRepository).findById(toAccountId);
        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT);
        verify(transactionRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void transfer_requestIsMoreThanBalance_throwsInsufficientBalanceException() {
        // Arrange
        Long fromAccountId = 100L;
        Long toAccountId = 200L;
        String idempotencyKey = "idem-transfer-1";
        TransferRequest transferRequest = new TransferRequest(toAccountId, new BigDecimal("130.00"));

        Account fromAccount = createAccount(createUser("FromUser", "from@example.com"), "TRY", "100.00");
        Account toAccount = createAccount(createUser("ToUser", "to@example.com"), "TRY", "100.00");

        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));

        // Act + Assert
        assertThrows(
                InsufficientBalanceException.class,
                () -> accountService.transfer(fromAccountId, transferRequest, idempotencyKey)
        );

        verify(accountRepository).findById(fromAccountId);
        verify(accountRepository).findById(toAccountId);
        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT);
        verify(transactionRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void transfer_existingIdempotentTransactionWithDifferentPayload_throwsIdempotencyConflictException() {
        // Arrange
        Long fromAccountId = 100L;
        Long toAccountId = 200L;
        String idempotencyKey = "idem-deposit-1";

        TransferRequest transferRequest = new TransferRequest(toAccountId, new BigDecimal("40.00"));
        Account fromAccount = createAccount(createUser("From User", "from@example.com"), "TRY", "100.00");

        Transaction existingTransaction = createTransaction(
                fromAccount,
                TransactionType.TRANSFER_OUT,
                TransactionStatus.SUCCESS,
                new BigDecimal("40.00"),
                new BigDecimal("140.00"),
                null,
                idempotencyKey,
                null,
                "different-hash"
        );

        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT))
                .thenReturn(Optional.of(existingTransaction));

        // Act + Assert
        assertThrows(
                IdempotencyConflictException.class,
                () -> accountService.transfer(fromAccountId, transferRequest, idempotencyKey)
        );

        verify(transactionRepository).findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT);
        verify(transactionRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    @Test
    void transfer_transactionSaveThrowsDataIntegrityViolation_returnsExistingTransactionBalance() {
        // Arrange
        Long fromAccountId = 100L;
        Long toAccountId = 200L;
        String idempotencyKey = "idem-deposit-1";
        BigDecimal amount = new BigDecimal("40.00");

        TransferRequest transferRequest = new TransferRequest(toAccountId, amount);
        Account fromAccount = createAccount(createUser("From User", "from@example.com"), "TRY", "100.00");
        Account toAccount = createAccount(createUser("To User", "to@example.com"), "TRY", "100.00");

        String requestHash = hashDeposit(fromAccountId, amount);
        Transaction existingTransaction = createTransaction(
                fromAccount,
                TransactionType.TRANSFER_OUT,
                TransactionStatus.SUCCESS,
                amount,
                new BigDecimal("140.00"),
                null,
                idempotencyKey,
                null,
                requestHash
        );

        when(accountRepository.findById(fromAccountId)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccountId)).thenReturn(Optional.of(toAccount));
        when(transactionRepository.findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingTransaction));
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(transactionRepository).save(any(Transaction.class));


        // Act
        TransferResponse transferResponse = accountService.transfer(fromAccountId, transferRequest, idempotencyKey);

        // Assert
        assertNotNull(transferResponse);
        assertEquals(new BigDecimal("40.00"), transferResponse.amount());

        verify(accountRepository).findById(fromAccountId);
        verify(accountRepository).findById(toAccountId);
        verify(transactionRepository, times(2))
                .findByIdempotencyKeyAndType(idempotencyKey, TransactionType.TRANSFER_OUT);
        verify(transactionRepository).save(any(Transaction.class));
        verifyNoMoreInteractions(userRepository, accountRepository, transactionRepository);
    }

    private User createUser(String fullName, String email) {
        return new User(fullName, email);
    }

    private Account createAccount(User user, String currency, String initialBalance) {
        Account account = new Account(user, currency);
        account.increaseBalance(new BigDecimal(initialBalance));
        return account;
    }

    private Transaction createTransaction(
            Account account,
            TransactionType transactionType,
            TransactionStatus transactionStatus,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String referenceId,
            String idempotencyKey,
            Long counterpartyAccountId,
            String requestHash
    ) {
        return new Transaction(
                account,
                transactionType,
                transactionStatus,
                amount,
                balanceAfter,
                referenceId,
                idempotencyKey,
                counterpartyAccountId,
                requestHash
        );
    }

    private String hashDeposit(Long accountId, BigDecimal amount) {
        String canonical = "DEPOSIT|accountId=" + accountId + "|amount=" + normalizeAmount(amount);
        return sha256Hex(canonical);
    }

    private String hashWithdraw(Long accountId, BigDecimal amount) {
        String canonical = "WITHDRAW|accountId=" + accountId + "|amount=" + normalizeAmount(amount);
        return sha256Hex(canonical);
    }

    private String hashTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        String canonical = "TRANSFER|from=" + fromAccountId + "|to=" + toAccountId + "|amount=" + normalizeAmount(amount);
        return sha256Hex(canonical);
    }

    private String normalizeAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder stringBuilder = new StringBuilder();
            for (byte hashByte : hashBytes) {
                stringBuilder.append(String.format("%02x", hashByte));
            }
            return stringBuilder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}