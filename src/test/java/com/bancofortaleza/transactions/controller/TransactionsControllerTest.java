package com.bancofortaleza.transactions.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancofortaleza.transactions.services.TransactionService;
import com.bff.services.server.models.AccountStatementReportResponse;
import com.bff.services.server.models.ConceptTransaction;
import com.bff.services.server.models.Status;
import com.bff.services.server.models.TransactionCreateRequest;
import com.bff.services.server.models.TransactionResponse;
import com.bff.services.server.models.TransactionStatusUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class TransactionsControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionsController controller;

    @Test
    void createTransactionShouldReturnServiceResponse() {
        // Arrange
        TransactionCreateRequest request = new TransactionCreateRequest();
        TransactionResponse response = new TransactionResponse().id(1);
        when(transactionService.createTransaction(request)).thenReturn(response);

        // Act
        ResponseEntity<TransactionResponse> result = controller.createTransaction("ip", "session", 1, request);

        // Assert
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void getTransactionByIdShouldReturnServiceResponse() {
        // Arrange
        TransactionResponse response = new TransactionResponse().id(1);
        when(transactionService.getTransactionById(1)).thenReturn(response);

        // Act
        ResponseEntity<TransactionResponse> result = controller.getTransactionById("ip", "session", 1, 1);

        // Assert
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void listTransactionsShouldReturnPageHeadersAndContent() {
        // Arrange
        TransactionResponse transaction = new TransactionResponse().id(1);
        when(transactionService.listTransactions(2, 5, "salary", 7, ConceptTransaction.CREDIT, Status.ACTIVE))
            .thenReturn(new PageImpl<>(List.of(transaction), org.springframework.data.domain.PageRequest.of(1, 5), 12));

        // Act
        ResponseEntity<List<TransactionResponse>> result = controller.listTransactions(
            "ip",
            "session",
            1,
            2,
            5,
            "salary",
            7,
            ConceptTransaction.CREDIT,
            Status.ACTIVE
        );

        // Assert
        assertThat(result.getBody()).containsExactly(transaction);
        assertThat(result.getHeaders().getFirst("x-total-count")).isEqualTo("12");
        assertThat(result.getHeaders().getFirst("x-page")).isEqualTo("2");
        assertThat(result.getHeaders().getFirst("x-page-size")).isEqualTo("5");
        assertThat(result.getHeaders().getFirst("x-total-pages")).isEqualTo("3");
    }

    @Test
    void updateTransactionStatusShouldReturnServiceResponse() {
        // Arrange
        TransactionStatusUpdateRequest request = new TransactionStatusUpdateRequest().status(Status.INACTIVE);
        TransactionResponse response = new TransactionResponse().id(1);
        when(transactionService.updateTransactionStatus(1, request)).thenReturn(response);

        // Act
        ResponseEntity<TransactionResponse> result = controller.updateTransactionStatus("ip", "session", 1, 1, request);

        // Assert
        assertThat(result.getBody()).isSameAs(response);
        verify(transactionService).updateTransactionStatus(1, request);
    }

    @Test
    void generateAccountStatementReportShouldReturnServiceResponse() {
        // Arrange
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);
        AccountStatementReportResponse response = new AccountStatementReportResponse().idUser(10);
        when(transactionService.generateAccountStatementReport(10, start, end)).thenReturn(response);

        // Act
        ResponseEntity<AccountStatementReportResponse> result = controller.generateAccountStatementReport(
            "ip",
            "session",
            1,
            10,
            start,
            end
        );

        // Assert
        assertThat(result.getBody()).isSameAs(response);
    }
}
