package com.bancofortaleza.transactions.services.mapper;

import com.bancofortaleza.transactions.repository.transactions.entity.TransactionEntity;
import com.bff.services.server.models.TransactionCreateRequest;
import com.bff.services.server.models.TransactionResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "idAccount", source = "account.id")
    TransactionResponse toTransactionResponse(TransactionEntity transaction);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TransactionEntity toTransactionEntity(TransactionCreateRequest transactionCreateRequest);

    List<TransactionResponse> toTransactionResponses(List<TransactionEntity> transactions);

    com.bancofortaleza.transactions.repository.transactions.entity.Status toEntityStatus(
        com.bff.services.server.models.Status status
    );

    com.bancofortaleza.transactions.repository.transactions.entity.ConceptTransaction toEntityConcept(
        com.bff.services.server.models.ConceptTransaction concept
    );

    default OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
