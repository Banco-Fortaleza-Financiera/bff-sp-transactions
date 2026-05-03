package com.bancofortaleza.transactions.repository.transactions.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TransactionEntityTest {

    @Test
    void prePersistShouldInitializeDefaultsWhenValuesAreMissing() {
        // Arrange
        TransactionEntity transaction = new TransactionEntity();
        transaction.setDescription(null);
        transaction.setStatus(null);

        // Act
        transaction.prePersist();

        // Assert
        assertThat(transaction.getDescription()).isEmpty();
        assertThat(transaction.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(transaction.getCreatedAt()).isNotNull();
    }

    @Test
    void prePersistShouldKeepExistingValues() {
        // Arrange
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 1, 10, 0);
        TransactionEntity transaction = new TransactionEntity();
        transaction.setDescription("Existing");
        transaction.setStatus(Status.INACTIVE);
        transaction.setCreatedAt(createdAt);

        // Act
        transaction.prePersist();

        // Assert
        assertThat(transaction.getDescription()).isEqualTo("Existing");
        assertThat(transaction.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(transaction.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void preUpdateShouldSetUpdatedAt() {
        // Arrange
        TransactionEntity transaction = new TransactionEntity();

        // Act
        transaction.preUpdate();

        // Assert
        assertThat(transaction.getUpdatedAt()).isNotNull();
    }
}
