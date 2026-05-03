package com.bancofortaleza.transactions.repository.transactions;

import com.bancofortaleza.transactions.repository.transactions.entity.ConceptTransaction;
import com.bancofortaleza.transactions.repository.transactions.entity.Status;
import com.bancofortaleza.transactions.repository.transactions.entity.TransactionEntity;
import com.bancofortaleza.transactions.repository.transactions.jpa.TransactionJpaRepository;
import com.bancofortaleza.transactions.utils.PaginationUtils;
import com.bancofortaleza.transactions.utils.SpecificationUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class TransactionRepository {

    private final TransactionJpaRepository transactionJpaRepository;

    public Page<TransactionEntity> listTransactions(
        Integer xPage,
        Integer xPageSize,
        String search,
        Integer idAccount,
        ConceptTransaction concept,
        Status status
    ) {
        Pageable pageable = PaginationUtils.fromHeaders(xPage, xPageSize, Sort.by(Sort.Direction.ASC, "id"));
        return transactionJpaRepository.findAll(
            buildListTransactionsSpecification(search, idAccount, concept, status),
            pageable
        );
    }

    @Transactional
    public TransactionEntity createTransaction(TransactionEntity transaction) {
        return transactionJpaRepository.save(transaction);
    }

    public Optional<TransactionEntity> getTransactionById(Integer id) {
        return transactionJpaRepository.findById(id);
    }

    public BigDecimal sumDailyActiveDebits(Integer accountId, LocalDate date) {
        BigDecimal amount = transactionJpaRepository.sumAmountByAccountConceptStatusAndCreatedAtBetween(
            accountId,
            ConceptTransaction.DEBIT,
            Status.ACTIVE,
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay()
        );
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public List<TransactionEntity> findTransactionsByAccountsAndDateRange(
        Collection<Integer> accountIds,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        return transactionJpaRepository
            .findByAccountIdInAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(
                accountIds,
                startDate,
                endDate
            );
    }

    @Transactional
    public Optional<TransactionEntity> updateTransactionStatus(Integer id, Status status) {
        return transactionJpaRepository.findById(id)
            .map(transaction -> {
                transaction.setStatus(status);
                return transactionJpaRepository.save(transaction);
            });
    }

    private Specification<TransactionEntity> buildListTransactionsSpecification(
        String search,
        Integer idAccount,
        ConceptTransaction concept,
        Status status
    ) {
        return SpecificationUtils.<TransactionEntity>equalIfNotNull("status", status)
            .and(SpecificationUtils.equalIfNotNull("account.id", idAccount))
            .and(SpecificationUtils.equalIfNotNull("concept", concept))
            .and(SpecificationUtils.containsIgnoreCase(search, "description"));
    }
}
