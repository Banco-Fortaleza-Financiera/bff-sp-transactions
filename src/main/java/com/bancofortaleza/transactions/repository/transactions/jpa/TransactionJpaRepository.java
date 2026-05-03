package com.bancofortaleza.transactions.repository.transactions.jpa;

import com.bancofortaleza.transactions.repository.transactions.entity.ConceptTransaction;
import com.bancofortaleza.transactions.repository.transactions.entity.Status;
import com.bancofortaleza.transactions.repository.transactions.entity.TransactionEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, Integer>, JpaSpecificationExecutor<TransactionEntity> {

    @Query("""
        select sum(t.amount)
        from TransactionEntity t
        where t.account.id = :accountId
          and t.concept = :concept
          and t.status = :status
          and t.createdAt >= :startDate
          and t.createdAt < :endDate
          and lower(coalesce(t.description, '')) not like 'reverso de la operacion%'
        """)
    BigDecimal sumAmountByAccountConceptStatusAndCreatedAtBetween(
        @Param("accountId") Integer accountId,
        @Param("concept") ConceptTransaction concept,
        @Param("status") Status status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    List<TransactionEntity> findByAccountIdInAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(
        Collection<Integer> accountIds,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
}
