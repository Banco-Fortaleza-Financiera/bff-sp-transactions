package com.bancofortaleza.transactions.repository.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancofortaleza.transactions.repository.transactions.entity.ConceptTransaction;
import com.bancofortaleza.transactions.repository.transactions.entity.Status;
import com.bancofortaleza.transactions.repository.transactions.entity.TransactionEntity;
import com.bancofortaleza.transactions.repository.transactions.jpa.TransactionJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TransactionRepositoryTest {

    @Mock
    private TransactionJpaRepository transactionJpaRepository;

    @InjectMocks
    private TransactionRepository transactionRepository;

    @Test
    void listTransactionsShouldBuildPageableAndDelegateToJpaRepository() {
        // Arrange
        Page<TransactionEntity> page = new PageImpl<>(List.of(new TransactionEntity()));
        when(transactionJpaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        // Act
        Page<TransactionEntity> result = transactionRepository.listTransactions(
            2,
            25,
            "salary",
            7,
            ConceptTransaction.CREDIT,
            Status.ACTIVE
        );

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionJpaRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(result).isSameAs(page);
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(25);
    }

    @Test
    void createTransactionAndGetByIdShouldDelegateToJpaRepository() {
        // Arrange
        TransactionEntity transaction = new TransactionEntity();
        when(transactionJpaRepository.save(transaction)).thenReturn(transaction);
        when(transactionJpaRepository.findById(1)).thenReturn(Optional.of(transaction));

        // Act / Assert
        assertThat(transactionRepository.createTransaction(transaction)).isSameAs(transaction);
        assertThat(transactionRepository.getTransactionById(1)).contains(transaction);
    }

    @Test
    void sumDailyActiveDebitsShouldReturnZeroWhenJpaReturnsNull() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 5, 3);
        when(transactionJpaRepository.sumAmountByAccountConceptStatusAndCreatedAtBetween(
            eq(7),
            eq(ConceptTransaction.DEBIT),
            eq(Status.ACTIVE),
            eq(date.atStartOfDay()),
            eq(date.plusDays(1).atStartOfDay())
        )).thenReturn(null);

        // Act
        BigDecimal result = transactionRepository.sumDailyActiveDebits(7, date);

        // Assert
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void sumDailyActiveDebitsShouldReturnJpaAmountWhenPresent() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 5, 3);
        when(transactionJpaRepository.sumAmountByAccountConceptStatusAndCreatedAtBetween(
            eq(7),
            eq(ConceptTransaction.DEBIT),
            eq(Status.ACTIVE),
            any(LocalDateTime.class),
            any(LocalDateTime.class)
        )).thenReturn(new BigDecimal("55.25"));

        // Act
        BigDecimal result = transactionRepository.sumDailyActiveDebits(7, date);

        // Assert
        assertThat(result).isEqualByComparingTo("55.25");
    }

    @Test
    void findTransactionsByAccountsAndDateRangeShouldDelegateToJpaRepository() {
        // Arrange
        List<Integer> accountIds = List.of(1, 2);
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 5, 2, 0, 0);
        List<TransactionEntity> transactions = List.of(new TransactionEntity());
        when(transactionJpaRepository.findByAccountIdInAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(
            accountIds,
            start,
            end
        )).thenReturn(transactions);

        // Act
        List<TransactionEntity> result = transactionRepository.findTransactionsByAccountsAndDateRange(accountIds, start, end);

        // Assert
        assertThat(result).isSameAs(transactions);
    }

    @Test
    void updateTransactionStatusShouldSaveWhenTransactionExists() {
        // Arrange
        TransactionEntity transaction = new TransactionEntity();
        transaction.setStatus(Status.ACTIVE);
        when(transactionJpaRepository.findById(1)).thenReturn(Optional.of(transaction));
        when(transactionJpaRepository.save(transaction)).thenReturn(transaction);

        // Act
        Optional<TransactionEntity> result = transactionRepository.updateTransactionStatus(1, Status.INACTIVE);

        // Assert
        assertThat(result).contains(transaction);
        assertThat(transaction.getStatus()).isEqualTo(Status.INACTIVE);
        verify(transactionJpaRepository).save(transaction);
    }

    @Test
    void updateTransactionStatusShouldReturnEmptyWhenTransactionDoesNotExist() {
        // Arrange
        when(transactionJpaRepository.findById(99)).thenReturn(Optional.empty());

        // Act
        Optional<TransactionEntity> result = transactionRepository.updateTransactionStatus(99, Status.INACTIVE);

        // Assert
        assertThat(result).isEmpty();
    }
}
