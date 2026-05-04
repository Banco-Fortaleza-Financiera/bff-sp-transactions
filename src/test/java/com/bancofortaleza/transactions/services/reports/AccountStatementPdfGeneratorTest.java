package com.bancofortaleza.transactions.services.reports;

import static org.assertj.core.api.Assertions.assertThat;

import com.bff.services.server.models.AccountStatementAccount;
import com.bff.services.server.models.AccountStatementReportResponse;
import com.bff.services.server.models.AccountStatementTransaction;
import com.bff.services.server.models.ConceptTransaction;
import com.bff.services.server.models.Status;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountStatementPdfGeneratorTest {

    private final AccountStatementPdfGenerator generator = new AccountStatementPdfGenerator();

    @Test
    void generateShouldCreatePdfForAccountsWithTransactions() {
        // Arrange
        AccountStatementTransaction debit = new AccountStatementTransaction()
            .id(1)
            .amount(new BigDecimal("25.00"))
            .description("ATM")
            .concept(ConceptTransaction.DEBIT)
            .status(Status.INACTIVE)
            .transactionDate(OffsetDateTime.of(2026, 5, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        AccountStatementTransaction credit = new AccountStatementTransaction()
            .id(2)
            .amount(new BigDecimal("100.00"))
            .description(null)
            .concept(ConceptTransaction.CREDIT)
            .status(Status.ACTIVE)
            .transactionDate(null);
        AccountStatementReportResponse report = baseReport()
            .accounts(List.of(account("001", List.of(debit, credit))));

        // Act
        byte[] pdf = generator.generate(report);

        // Assert
        assertThat(pdf).startsWith("%PDF".getBytes());
        assertThat(pdf.length).isGreaterThan(1000);
    }

    @Test
    void generateShouldCreatePdfForAccountWithoutTransactionsAndNullValues() {
        // Arrange
        AccountStatementReportResponse report = new AccountStatementReportResponse()
            .idUser(10)
            .startDate(null)
            .endDate(null)
            .generatedAt(null)
            .totalDebits(null)
            .totalCredits(null)
            .accounts(List.of(account("002", List.of())));

        // Act
        byte[] pdf = generator.generate(report);

        // Assert
        assertThat(pdf).startsWith("%PDF".getBytes());
    }

    private static AccountStatementReportResponse baseReport() {
        return new AccountStatementReportResponse()
            .idUser(10)
            .startDate(LocalDate.of(2026, 5, 1))
            .endDate(LocalDate.of(2026, 5, 31))
            .generatedAt(OffsetDateTime.of(2026, 5, 3, 12, 0, 0, 0, ZoneOffset.UTC))
            .totalDebits(new BigDecimal("25.00"))
            .totalCredits(new BigDecimal("100.00"));
    }

    private static AccountStatementAccount account(String accountNumber, List<AccountStatementTransaction> transactions) {
        return new AccountStatementAccount()
            .idAccount(1)
            .accountNumber(accountNumber)
            .balance(new BigDecimal("75.00"))
            .totalDebits(new BigDecimal("25.00"))
            .totalCredits(new BigDecimal("100.00"))
            .transactions(transactions);
    }
}
