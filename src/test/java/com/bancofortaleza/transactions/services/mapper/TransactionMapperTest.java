package com.bancofortaleza.transactions.services.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bancofortaleza.transactions.repository.accounts.entity.AccountEntity;
import com.bancofortaleza.transactions.repository.transactions.entity.ConceptTransaction;
import com.bancofortaleza.transactions.repository.transactions.entity.Status;
import com.bancofortaleza.transactions.repository.transactions.entity.TransactionEntity;
import com.bff.services.server.models.TransactionCreateRequest;
import com.bff.services.server.models.TransactionResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class TransactionMapperTest {

    private final TransactionMapper mapper = Mappers.getMapper(TransactionMapper.class);

    @Test
    void toTransactionResponseShouldMapEntityFieldsAndAccountId() {
        // Arrange
        AccountEntity account = new AccountEntity();
        account.setId(7);

        TransactionEntity entity = new TransactionEntity();
        entity.setId(10);
        entity.setAccount(account);
        entity.setAmount(new BigDecimal("125.50"));
        entity.setDescription("Salary payment");
        entity.setConcept(ConceptTransaction.CREDIT);
        entity.setStatus(Status.ACTIVE);
        entity.setCreatedAt(LocalDateTime.of(2026, 5, 1, 10, 30));
        entity.setUpdatedAt(LocalDateTime.of(2026, 5, 1, 11, 45));

        // Act
        TransactionResponse response = mapper.toTransactionResponse(entity);

        // Assert
        assertThat(response.getId()).isEqualTo(10);
        assertThat(response.getIdAccount()).isEqualTo(7);
        assertThat(response.getAmount()).isEqualByComparingTo("125.50");
        assertThat(response.getDescription()).isEqualTo("Salary payment");
        assertThat(response.getConcept()).isEqualTo(com.bff.services.server.models.ConceptTransaction.CREDIT);
        assertThat(response.getStatus()).isEqualTo(com.bff.services.server.models.Status.ACTIVE);
        assertThat(response.getCreatedAt()).isEqualTo(OffsetDateTime.of(2026, 5, 1, 10, 30, 0, 0, ZoneOffset.UTC));
        assertThat(response.getUpdatedAt()).isEqualTo(OffsetDateTime.of(2026, 5, 1, 11, 45, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void toTransactionEntityShouldIgnoreGeneratedFieldsAndAccount() {
        // Arrange
        TransactionCreateRequest request = new TransactionCreateRequest()
            .idAccount(7)
            .amount(new BigDecimal("25.00"))
            .description("ATM withdrawal")
            .concept(com.bff.services.server.models.ConceptTransaction.DEBIT)
            .status(com.bff.services.server.models.Status.INACTIVE);

        // Act
        TransactionEntity entity = mapper.toTransactionEntity(request);

        // Assert
        assertThat(entity.getId()).isNull();
        assertThat(entity.getAccount()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.getAmount()).isEqualByComparingTo("25.00");
        assertThat(entity.getDescription()).isEqualTo("ATM withdrawal");
        assertThat(entity.getConcept()).isEqualTo(ConceptTransaction.DEBIT);
        assertThat(entity.getStatus()).isEqualTo(Status.INACTIVE);
    }

    @Test
    void shouldMapEnumsListsAndNullDates() {
        // Arrange
        TransactionEntity entity = new TransactionEntity();
        entity.setId(1);
        entity.setConcept(ConceptTransaction.DEBIT);
        entity.setStatus(Status.ACTIVE);

        // Act / Assert
        assertThat(mapper.toEntityConcept(com.bff.services.server.models.ConceptTransaction.DEBIT))
            .isEqualTo(ConceptTransaction.DEBIT);
        assertThat(mapper.toEntityStatus(com.bff.services.server.models.Status.INACTIVE))
            .isEqualTo(Status.INACTIVE);
        assertThat(mapper.toTransactionResponses(List.of(entity))).hasSize(1);
        assertThat(mapper.toOffsetDateTime(null)).isNull();
    }
}
