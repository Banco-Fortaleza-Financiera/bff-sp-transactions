package com.bancofortaleza.transactions.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String code,
    String message,
    String path,
    List<FieldError> details
) {

    public record FieldError(String field, String message) {
    }
}
