package com.bancofortaleza.transactions.services;

import com.bff.services.server.models.ConceptTransaction;
import com.bff.services.server.models.AccountStatementReportResponse;
import com.bff.services.server.models.Status;
import com.bff.services.server.models.TransactionCreateRequest;
import com.bff.services.server.models.TransactionResponse;
import com.bff.services.server.models.TransactionStatusUpdateRequest;
import java.time.LocalDate;
import org.springframework.data.domain.Page;

public interface TransactionService {

    Page<TransactionResponse> listTransactions(
        Integer xPage,
        Integer xPageSize,
        String search,
        Integer idAccount,
        ConceptTransaction concept,
        Status status
    );

    TransactionResponse createTransaction(TransactionCreateRequest transactionCreateRequest);

    TransactionResponse getTransactionById(Integer id);

    TransactionResponse updateTransactionStatus(Integer id, TransactionStatusUpdateRequest transactionStatusUpdateRequest);

    AccountStatementReportResponse generateAccountStatementReport(Integer idUser, LocalDate startDate, LocalDate endDate);
}
