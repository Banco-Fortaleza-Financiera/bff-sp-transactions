package com.bancofortaleza.transactions.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancofortaleza.transactions.domain.exceptions.ApiException;
import com.bancofortaleza.transactions.repository.accounts.AccountRepository;
import com.bancofortaleza.transactions.repository.accounts.entity.AccountEntity;
import com.bancofortaleza.transactions.repository.accounts.entity.AccountStatus;
import com.bancofortaleza.transactions.repository.transactions.TransactionRepository;
import com.bancofortaleza.transactions.repository.transactions.entity.ConceptTransaction;
import com.bancofortaleza.transactions.repository.transactions.entity.Status;
import com.bancofortaleza.transactions.repository.transactions.entity.TransactionEntity;
import com.bancofortaleza.transactions.services.mapper.TransactionMapper;
import com.bancofortaleza.transactions.services.reports.AccountStatementPdfGenerator;
import com.bff.services.server.models.AccountStatementReportResponse;
import com.bff.services.server.models.TransactionCreateRequest;
import com.bff.services.server.models.TransactionResponse;
import com.bff.services.server.models.TransactionStatusUpdateRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private AccountStatementPdfGenerator accountStatementPdfGenerator;

    @InjectMocks
    private TransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "dailyWithdrawalLimit", new BigDecimal("500.00"));
    }

    @Test
    void listTransactionsShouldMapFiltersAndResponses() {
        // Arrange
        TransactionEntity entity = transaction(1, account(7, "001", "100.00", AccountStatus.ACTIVE), "25.00", ConceptTransaction.CREDIT, Status.ACTIVE);
        TransactionResponse response = response(1);
        when(transactionMapper.toEntityConcept(com.bff.services.server.models.ConceptTransaction.CREDIT))
            .thenReturn(ConceptTransaction.CREDIT);
        when(transactionMapper.toEntityStatus(com.bff.services.server.models.Status.ACTIVE)).thenReturn(Status.ACTIVE);
        when(transactionRepository.listTransactions(1, 10, "salary", 7, ConceptTransaction.CREDIT, Status.ACTIVE))
            .thenReturn(new PageImpl<>(List.of(entity)));
        when(transactionMapper.toTransactionResponse(entity)).thenReturn(response);

        // Act
        Page<TransactionResponse> result = service.listTransactions(
            1,
            10,
            "salary",
            7,
            com.bff.services.server.models.ConceptTransaction.CREDIT,
            com.bff.services.server.models.Status.ACTIVE
        );

        // Assert
        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void createTransactionShouldCreditAccountAndReturnSavedTransaction() {
        // Arrange
        TransactionCreateRequest request = createRequest(7, "125.00", com.bff.services.server.models.ConceptTransaction.CREDIT);
        AccountEntity account = account(7, "001", "100.00", AccountStatus.ACTIVE);
        TransactionEntity entity = transaction(null, null, "125.00", ConceptTransaction.CREDIT, null);
        TransactionResponse response = response(10);

        when(transactionMapper.toTransactionEntity(request)).thenReturn(entity);
        when(accountRepository.getAccountById(7)).thenReturn(Optional.of(account));
        when(transactionRepository.createTransaction(entity)).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toTransactionResponse(entity)).thenReturn(response);

        // Act
        TransactionResponse result = service.createTransaction(request);

        // Assert
        assertThat(result).isSameAs(response);
        assertThat(account.getBalance()).isEqualByComparingTo("225.00");
        assertThat(entity.getAccount()).isSameAs(account);
        verify(accountRepository).save(account);
        verify(transactionRepository, never()).sumDailyActiveDebits(any(), any());
    }

    @Test
    void createTransactionShouldDebitAccountWhenBalanceAndDailyLimitAllowIt() {
        // Arrange
        TransactionCreateRequest request = createRequest(7, "100.00", com.bff.services.server.models.ConceptTransaction.DEBIT);
        AccountEntity account = account(7, "001", "300.00", AccountStatus.ACTIVE);
        TransactionEntity entity = transaction(null, null, "100.00", ConceptTransaction.DEBIT, Status.ACTIVE);

        when(transactionMapper.toTransactionEntity(request)).thenReturn(entity);
        when(accountRepository.getAccountById(7)).thenReturn(Optional.of(account));
        when(transactionRepository.sumDailyActiveDebits(eq(7), any(LocalDate.class))).thenReturn(new BigDecimal("250.00"));
        when(transactionRepository.createTransaction(entity)).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toTransactionResponse(entity)).thenReturn(response(10));

        // Act
        service.createTransaction(request);

        // Assert
        assertThat(account.getBalance()).isEqualByComparingTo("200.00");
        verify(accountRepository).save(account);
    }

    @Test
    void createTransactionShouldNotApplyInactiveTransactionToBalance() {
        // Arrange
        TransactionCreateRequest request = createRequest(7, "100.00", com.bff.services.server.models.ConceptTransaction.DEBIT);
        AccountEntity account = account(7, "001", "300.00", AccountStatus.ACTIVE);
        TransactionEntity entity = transaction(null, null, "100.00", ConceptTransaction.DEBIT, Status.INACTIVE);

        when(transactionMapper.toTransactionEntity(request)).thenReturn(entity);
        when(accountRepository.getAccountById(7)).thenReturn(Optional.of(account));
        when(transactionRepository.createTransaction(entity)).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toTransactionResponse(entity)).thenReturn(response(10));

        // Act
        service.createTransaction(request);

        // Assert
        assertThat(account.getBalance()).isEqualByComparingTo("300.00");
        verify(accountRepository, never()).save(account);
    }

    @Test
    void createTransactionShouldFailWhenAccountDoesNotExist() {
        // Arrange
        TransactionCreateRequest request = createRequest(99, "10.00", com.bff.services.server.models.ConceptTransaction.CREDIT);
        when(transactionMapper.toTransactionEntity(request)).thenReturn(new TransactionEntity());
        when(accountRepository.getAccountById(99)).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> service.createTransaction(request))
            .isInstanceOf(ApiException.class)
            .hasMessage("Account with id 99 was not found")
            .extracting("code")
            .isEqualTo("ACCOUNT_NOT_FOUND");
    }

    @Test
    void createTransactionShouldFailWhenAccountIsInactive() {
        // Arrange
        TransactionCreateRequest request = createRequest(7, "10.00", com.bff.services.server.models.ConceptTransaction.CREDIT);
        when(transactionMapper.toTransactionEntity(request)).thenReturn(new TransactionEntity());
        when(accountRepository.getAccountById(7)).thenReturn(Optional.of(account(7, "001", "100.00", AccountStatus.INACTIVE)));

        // Act / Assert
        assertThatThrownBy(() -> service.createTransaction(request))
            .isInstanceOf(ApiException.class)
            .extracting("code")
            .isEqualTo("ACCOUNT_INACTIVE");
    }

    @Test
    void createTransactionShouldFailWhenBalanceIsNotAvailable() {
        // Arrange
        TransactionCreateRequest request = createRequest(7, "150.00", com.bff.services.server.models.ConceptTransaction.DEBIT);
        TransactionEntity entity = transaction(null, null, "150.00", ConceptTransaction.DEBIT, Status.ACTIVE);
        when(transactionMapper.toTransactionEntity(request)).thenReturn(entity);
        when(accountRepository.getAccountById(7)).thenReturn(Optional.of(account(7, "001", "100.00", AccountStatus.ACTIVE)));

        // Act / Assert
        assertThatThrownBy(() -> service.createTransaction(request))
            .isInstanceOf(ApiException.class)
            .extracting("code")
            .isEqualTo("BALANCE_NOT_AVAILABLE");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void createTransactionShouldFailWhenDailyWithdrawalLimitIsExceeded() {
        // Arrange
        TransactionCreateRequest request = createRequest(7, "100.00", com.bff.services.server.models.ConceptTransaction.DEBIT);
        TransactionEntity entity = transaction(null, null, "100.00", ConceptTransaction.DEBIT, Status.ACTIVE);
        when(transactionMapper.toTransactionEntity(request)).thenReturn(entity);
        when(accountRepository.getAccountById(7)).thenReturn(Optional.of(account(7, "001", "300.00", AccountStatus.ACTIVE)));
        when(transactionRepository.sumDailyActiveDebits(eq(7), any(LocalDate.class))).thenReturn(new BigDecimal("450.01"));

        // Act / Assert
        assertThatThrownBy(() -> service.createTransaction(request))
            .isInstanceOf(ApiException.class)
            .extracting("code")
            .isEqualTo("DAILY_WITHDRAWAL_LIMIT_EXCEEDED");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void getTransactionByIdShouldReturnMappedTransaction() {
        // Arrange
        TransactionEntity entity = transaction(1, account(7, "001", "100.00", AccountStatus.ACTIVE), "10.00", ConceptTransaction.CREDIT, Status.ACTIVE);
        TransactionResponse response = response(1);
        when(transactionRepository.getTransactionById(1)).thenReturn(Optional.of(entity));
        when(transactionMapper.toTransactionResponse(entity)).thenReturn(response);

        // Act
        TransactionResponse result = service.getTransactionById(1);

        // Assert
        assertThat(result).isSameAs(response);
    }

    @Test
    void getTransactionByIdShouldFailWhenTransactionDoesNotExist() {
        // Arrange
        when(transactionRepository.getTransactionById(99)).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> service.getTransactionById(99))
            .isInstanceOf(ApiException.class)
            .extracting("code")
            .isEqualTo("TRANSACTION_NOT_FOUND");
    }

    @Test
    void updateTransactionStatusShouldReturnCurrentTransactionWhenStatusDoesNotChange() {
        // Arrange
        TransactionEntity entity = transaction(1, account(7, "001", "100.00", AccountStatus.ACTIVE), "10.00", ConceptTransaction.CREDIT, Status.ACTIVE);
        TransactionStatusUpdateRequest request = new TransactionStatusUpdateRequest().status(com.bff.services.server.models.Status.ACTIVE);
        TransactionResponse response = response(1);
        when(transactionMapper.toEntityStatus(com.bff.services.server.models.Status.ACTIVE)).thenReturn(Status.ACTIVE);
        when(transactionRepository.getTransactionById(1)).thenReturn(Optional.of(entity));
        when(transactionMapper.toTransactionResponse(entity)).thenReturn(response);

        // Act
        TransactionResponse result = service.updateTransactionStatus(1, request);

        // Assert
        assertThat(result).isSameAs(response);
        verify(transactionRepository, never()).createTransaction(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void updateTransactionStatusShouldFailWhenTryingToReactivateInactiveTransaction() {
        // Arrange
        TransactionEntity entity = transaction(1, account(7, "001", "100.00", AccountStatus.ACTIVE), "10.00", ConceptTransaction.CREDIT, Status.INACTIVE);
        TransactionStatusUpdateRequest request = new TransactionStatusUpdateRequest().status(com.bff.services.server.models.Status.ACTIVE);
        when(transactionMapper.toEntityStatus(com.bff.services.server.models.Status.ACTIVE)).thenReturn(Status.ACTIVE);
        when(transactionRepository.getTransactionById(1)).thenReturn(Optional.of(entity));

        // Act / Assert
        assertThatThrownBy(() -> service.updateTransactionStatus(1, request))
            .isInstanceOf(ApiException.class)
            .extracting("code")
            .isEqualTo("TRANSACTION_CANNOT_BE_REACTIVATED");
    }

    @Test
    void updateTransactionStatusShouldCreateReversalAndPersistUpdatedTransaction() {
        // Arrange
        AccountEntity account = account(7, "001", "100.00", AccountStatus.ACTIVE);
        TransactionEntity entity = transaction(1, account, "30.00", ConceptTransaction.DEBIT, Status.ACTIVE);
        TransactionStatusUpdateRequest request = new TransactionStatusUpdateRequest().status(com.bff.services.server.models.Status.INACTIVE);
        TransactionResponse response = response(1);

        when(transactionMapper.toEntityStatus(com.bff.services.server.models.Status.INACTIVE)).thenReturn(Status.INACTIVE);
        when(transactionRepository.getTransactionById(1)).thenReturn(Optional.of(entity));
        when(transactionRepository.createTransaction(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toTransactionResponse(entity)).thenReturn(response);

        // Act
        TransactionResponse result = service.updateTransactionStatus(1, request);

        // Assert
        assertThat(result).isSameAs(response);
        assertThat(account.getBalance()).isEqualByComparingTo("130.00");
        assertThat(entity.getStatus()).isEqualTo(Status.INACTIVE);
        verify(accountRepository).save(account);
        verify(transactionRepository).createTransaction(entity);
    }

    @Test
    void updateTransactionStatusShouldFailWhenAccountIsInactive() {
        // Arrange
        AccountEntity account = account(7, "001", "100.00", AccountStatus.INACTIVE);
        TransactionEntity entity = transaction(1, account, "30.00", ConceptTransaction.DEBIT, Status.ACTIVE);
        TransactionStatusUpdateRequest request = new TransactionStatusUpdateRequest().status(com.bff.services.server.models.Status.INACTIVE);

        when(transactionMapper.toEntityStatus(com.bff.services.server.models.Status.INACTIVE)).thenReturn(Status.INACTIVE);
        when(transactionRepository.getTransactionById(1)).thenReturn(Optional.of(entity));

        // Act / Assert
        assertThatThrownBy(() -> service.updateTransactionStatus(1, request))
            .isInstanceOf(ApiException.class)
            .extracting("code")
            .isEqualTo("ACCOUNT_INACTIVE");
    }

    @Test
    void generateAccountStatementReportShouldBuildTotalsAndPdf() {
        // Arrange
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);
        AccountEntity secondAccount = account(2, "002", "50.00", AccountStatus.ACTIVE);
        AccountEntity firstAccount = account(1, "001", null, AccountStatus.ACTIVE);
        TransactionEntity activeDebit = transaction(1, firstAccount, "25.00", ConceptTransaction.DEBIT, Status.ACTIVE);
        activeDebit.setCreatedAt(LocalDateTime.of(2026, 5, 2, 10, 0));
        TransactionEntity inactiveDebit = transaction(2, firstAccount, "100.00", ConceptTransaction.DEBIT, Status.INACTIVE);
        inactiveDebit.setCreatedAt(LocalDateTime.of(2026, 5, 2, 11, 0));
        TransactionEntity activeCredit = transaction(3, secondAccount, "40.00", ConceptTransaction.CREDIT, Status.ACTIVE);
        activeCredit.setCreatedAt(LocalDateTime.of(2026, 5, 3, 9, 0));

        when(accountRepository.findAccountsByUser(10)).thenReturn(List.of(secondAccount, firstAccount));
        when(transactionRepository.findTransactionsByAccountsAndDateRange(
            List.of(2, 1),
            start.atStartOfDay(),
            end.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(activeDebit, inactiveDebit, activeCredit));
        when(transactionMapper.toOffsetDateTime(activeDebit.getCreatedAt()))
            .thenReturn(OffsetDateTime.of(2026, 5, 2, 10, 0, 0, 0, ZoneOffset.UTC));
        when(transactionMapper.toOffsetDateTime(inactiveDebit.getCreatedAt()))
            .thenReturn(OffsetDateTime.of(2026, 5, 2, 11, 0, 0, 0, ZoneOffset.UTC));
        when(transactionMapper.toOffsetDateTime(activeCredit.getCreatedAt()))
            .thenReturn(OffsetDateTime.of(2026, 5, 3, 9, 0, 0, 0, ZoneOffset.UTC));
        when(accountStatementPdfGenerator.generate(any(AccountStatementReportResponse.class))).thenReturn(new byte[] {1, 2, 3});

        // Act
        AccountStatementReportResponse result = service.generateAccountStatementReport(10, start, end);

        // Assert
        assertThat(result.getIdUser()).isEqualTo(10);
        assertThat(result.getAccounts()).extracting("idAccount").containsExactly(1, 2);
        assertThat(result.getAccounts().get(0).getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getAccounts().get(0).getTotalDebits()).isEqualByComparingTo("25.00");
        assertThat(result.getAccounts().get(1).getTotalCredits()).isEqualByComparingTo("40.00");
        assertThat(result.getTotalDebits()).isEqualByComparingTo("25.00");
        assertThat(result.getTotalCredits()).isEqualByComparingTo("40.00");
        assertThat(result.getPdfBase64()).containsExactly(1, 2, 3);
    }

    @Test
    void generateAccountStatementReportShouldFailWhenDateRangeIsInvalid() {
        // Act / Assert
        assertThatThrownBy(() -> service.generateAccountStatementReport(
            10,
            LocalDate.of(2026, 5, 31),
            LocalDate.of(2026, 5, 1)
        ))
            .isInstanceOf(ApiException.class)
            .extracting("code")
            .isEqualTo("INVALID_DATE_RANGE");
    }

    @Test
    void generateAccountStatementReportShouldFailWhenCustomerHasNoAccounts() {
        // Arrange
        when(accountRepository.findAccountsByUser(10)).thenReturn(List.of());

        // Act / Assert
        assertThatThrownBy(() -> service.generateAccountStatementReport(
            10,
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 31)
        ))
            .isInstanceOf(ApiException.class)
            .extracting("code")
            .isEqualTo("CUSTOMER_ACCOUNTS_NOT_FOUND");
    }

    private static TransactionCreateRequest createRequest(
        Integer idAccount,
        String amount,
        com.bff.services.server.models.ConceptTransaction concept
    ) {
        return new TransactionCreateRequest()
            .idAccount(idAccount)
            .amount(new BigDecimal(amount))
            .concept(concept)
            .status(com.bff.services.server.models.Status.ACTIVE);
    }

    private static AccountEntity account(Integer id, String number, String balance, AccountStatus status) {
        AccountEntity account = new AccountEntity();
        account.setId(id);
        account.setIdUser(10);
        account.setAccountNumber(number);
        account.setBalance(balance == null ? null : new BigDecimal(balance));
        account.setStatus(status);
        return account;
    }

    private static TransactionEntity transaction(
        Integer id,
        AccountEntity account,
        String amount,
        ConceptTransaction concept,
        Status status
    ) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setId(id);
        transaction.setAccount(account);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDescription("transaction " + id);
        transaction.setConcept(concept);
        transaction.setStatus(status);
        return transaction;
    }

    private static TransactionResponse response(Integer id) {
        return new TransactionResponse().id(id);
    }
}
