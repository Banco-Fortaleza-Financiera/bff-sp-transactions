package com.bancofortaleza.transactions.services.impl;

import com.bancofortaleza.transactions.domain.exceptions.ApiException;
import com.bancofortaleza.transactions.repository.accounts.AccountRepository;
import com.bancofortaleza.transactions.repository.accounts.entity.AccountEntity;
import com.bancofortaleza.transactions.repository.accounts.entity.AccountStatus;
import com.bancofortaleza.transactions.repository.transactions.TransactionRepository;
import com.bancofortaleza.transactions.repository.transactions.entity.ConceptTransaction;
import com.bancofortaleza.transactions.repository.transactions.entity.Status;
import com.bancofortaleza.transactions.repository.transactions.entity.TransactionEntity;
import com.bancofortaleza.transactions.services.TransactionService;
import com.bancofortaleza.transactions.services.mapper.TransactionMapper;
import com.bff.services.server.models.TransactionCreateRequest;
import com.bff.services.server.models.TransactionResponse;
import com.bff.services.server.models.TransactionStatusUpdateRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    @Value("${transactions.daily-withdrawal-limit}")
    private BigDecimal dailyWithdrawalLimit;

    @Override
    @Cacheable(value = "transactions:list:page", key = "{#xPage, #xPageSize, #search, #idAccount, #concept, #status}")
    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(
        Integer xPage,
        Integer xPageSize,
        String search,
        Integer idAccount,
        com.bff.services.server.models.ConceptTransaction concept,
        com.bff.services.server.models.Status status
    ) {
        ConceptTransaction conceptEnum = transactionMapper.toEntityConcept(concept);
        Status statusEnum = transactionMapper.toEntityStatus(status);
        return transactionRepository.listTransactions(xPage, xPageSize, search, idAccount, conceptEnum, statusEnum)
            .map(transactionMapper::toTransactionResponse);
    }

    @Override
    @CacheEvict(value = {"transactions:list", "transactions:list:page", "transactions:detail"}, allEntries = true)
    @Transactional
    public TransactionResponse createTransaction(TransactionCreateRequest transactionCreateRequest) {
        TransactionEntity transaction = transactionMapper.toTransactionEntity(transactionCreateRequest);
        AccountEntity account = getAccountOrThrow(transactionCreateRequest.getIdAccount());
        transaction.setAccount(account);

        if (transaction.getStatus() == null || Status.ACTIVE.equals(transaction.getStatus())) {
            applyTransactionToBalance(account, transaction.getConcept(), transaction.getAmount(), true);
        }

        return transactionMapper.toTransactionResponse(transactionRepository.createTransaction(transaction));
    }

    @Override
    @Cacheable(value = "transactions:detail", key = "#id")
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Integer id) {
        return transactionMapper.toTransactionResponse(getTransactionOrThrow(id));
    }

    @Override
    @CacheEvict(value = {"transactions:list", "transactions:list:page", "transactions:detail"}, allEntries = true)
    @Transactional
    public TransactionResponse updateTransactionStatus(Integer id, TransactionStatusUpdateRequest transactionStatusUpdateRequest) {
        Status status = transactionMapper.toEntityStatus(transactionStatusUpdateRequest.getStatus());
        TransactionEntity transaction = getTransactionOrThrow(id);

        if (transaction.getStatus() == status) {
            return transactionMapper.toTransactionResponse(transaction);
        }

        if (Status.INACTIVE.equals(transaction.getStatus()) && Status.ACTIVE.equals(status)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "TRANSACTION_CANNOT_BE_REACTIVATED",
                "La transaccion inactiva no puede pasar a estado activo"
            );
        }

        validateActiveAccount(transaction.getAccount());

        ConceptTransaction reversalConcept = Status.ACTIVE.equals(status)
            ? transaction.getConcept()
            : inverseConcept(transaction.getConcept());

        applyTransactionToBalance(transaction.getAccount(), reversalConcept, transaction.getAmount(), false);
        transaction.setStatus(status);

        TransactionEntity reversal = new TransactionEntity();
        reversal.setAccount(transaction.getAccount());
        reversal.setAmount(transaction.getAmount());
        reversal.setConcept(reversalConcept);
        reversal.setStatus(Status.ACTIVE);
        reversal.setDescription("reverso de la operacion " + transaction.getId());
        transactionRepository.createTransaction(reversal);

        transaction = transactionRepository.createTransaction(transaction);
        return transactionMapper.toTransactionResponse(transaction);
    }

    private void applyTransactionToBalance(
        AccountEntity account,
        ConceptTransaction concept,
        BigDecimal amount,
        boolean enforceDailyWithdrawalLimit
    ) {
        BigDecimal currentBalance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();

        if (ConceptTransaction.DEBIT.equals(concept)) {
            validateAvailableBalance(currentBalance, amount);
            if (enforceDailyWithdrawalLimit) {
                validateDailyWithdrawalLimit(account.getId(), amount);
            }
            account.setBalance(currentBalance.subtract(amount));
        } else {
            account.setBalance(currentBalance.add(amount));
        }

        accountRepository.save(account);
    }

    private void validateAvailableBalance(BigDecimal currentBalance, BigDecimal amount) {
        if (currentBalance.compareTo(BigDecimal.ZERO) <= 0 || currentBalance.compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BALANCE_NOT_AVAILABLE", "Saldo no disponible");
        }
    }

    private void validateDailyWithdrawalLimit(Integer accountId, BigDecimal amount) {
        BigDecimal dailyDebits = transactionRepository.sumDailyActiveDebits(accountId, LocalDate.now());
        if (dailyDebits.add(amount).compareTo(dailyWithdrawalLimit) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DAILY_WITHDRAWAL_LIMIT_EXCEEDED", "Cupo diario excedido");
        }
    }

    private ConceptTransaction inverseConcept(ConceptTransaction concept) {
        return ConceptTransaction.DEBIT.equals(concept)
            ? ConceptTransaction.CREDIT
            : ConceptTransaction.DEBIT;
    }

    private TransactionEntity getTransactionOrThrow(Integer id) {
        return transactionRepository.getTransactionById(id)
            .orElseThrow(() -> transactionNotFoundException(id));
    }

    private AccountEntity getAccountOrThrow(Integer id) {
        AccountEntity account = accountRepository.getAccountById(id)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "ACCOUNT_NOT_FOUND",
                "Account with id " + id + " was not found"
            ));
        validateActiveAccount(account);
        return account;
    }

    private void validateActiveAccount(AccountEntity account) {
        if (AccountStatus.INACTIVE.equals(account.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ACCOUNT_INACTIVE", "Cuenta inactiva");
        }
    }

    private ApiException transactionNotFoundException(Integer id) {
        return new ApiException(
            HttpStatus.NOT_FOUND,
            "TRANSACTION_NOT_FOUND",
            "Transaction with id " + id + " was not found"
        );
    }
}
