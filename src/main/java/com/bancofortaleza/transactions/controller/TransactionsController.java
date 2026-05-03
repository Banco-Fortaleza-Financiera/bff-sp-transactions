package com.bancofortaleza.transactions.controller;

import com.bancofortaleza.transactions.security.AdminOnly;
import com.bancofortaleza.transactions.services.TransactionService;
import com.bff.services.server.SupportApi;
import com.bff.services.server.models.ConceptTransaction;
import com.bff.services.server.models.Status;
import com.bff.services.server.models.TransactionCreateRequest;
import com.bff.services.server.models.TransactionResponse;
import com.bff.services.server.models.TransactionStatusUpdateRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TransactionsController implements SupportApi {

    private final TransactionService transactionService;

    @Override
    @AdminOnly
    public ResponseEntity<TransactionResponse> createTransaction(
        String xDeviceIp,
        String xSession,
        Integer xUserid,
        TransactionCreateRequest transactionCreateRequest
    ) {
        return ResponseEntity.ok(transactionService.createTransaction(transactionCreateRequest));
    }

    @Override
    @AdminOnly
    public ResponseEntity<TransactionResponse> getTransactionById(
        String xDeviceIp,
        String xSession,
        Integer xUserid,
        Integer id
    ) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @Override
    @AdminOnly
    public ResponseEntity<List<TransactionResponse>> listTransactions(
        String xDeviceIp,
        String xSession,
        Integer xUserid,
        Integer xPage,
        Integer xPageSize,
        String search,
        Integer idAccount,
        ConceptTransaction concept,
        Status status
    ) {
        Page<TransactionResponse> transactions = transactionService.listTransactions(
            xPage,
            xPageSize,
            search,
            idAccount,
            concept,
            status
        );

        return pagedResponse(transactions);
    }

    @Override
    @AdminOnly
    public ResponseEntity<TransactionResponse> updateTransactionStatus(
        String xDeviceIp,
        String xSession,
        Integer xUserid,
        Integer id,
        TransactionStatusUpdateRequest transactionStatusUpdateRequest
    ) {
        return ResponseEntity.ok(transactionService.updateTransactionStatus(id, transactionStatusUpdateRequest));
    }

    private <T> ResponseEntity<List<T>> pagedResponse(Page<T> page) {
        return ResponseEntity.ok()
            .header("x-total-count", String.valueOf(page.getTotalElements()))
            .header("x-page", String.valueOf(page.getNumber() + 1))
            .header("x-page-size", String.valueOf(page.getSize()))
            .header("x-total-pages", String.valueOf(page.getTotalPages()))
            .body(page.getContent());
    }
}
