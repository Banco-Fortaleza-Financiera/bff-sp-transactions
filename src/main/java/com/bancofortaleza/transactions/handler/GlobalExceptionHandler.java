package com.bancofortaleza.transactions.handler;

import com.bancofortaleza.transactions.domain.exceptions.ApiException;
import com.bancofortaleza.transactions.domain.model.ErrorResponse;
import com.bancofortaleza.transactions.utils.AppLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        HttpStatus status = exception.getStatus();
        AppLogger.warn(
            GlobalExceptionHandler.class,
            "Handled API exception status={} code={}",
            status.value(),
            exception.getCode()
        );
        return buildResponse(status, exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<ErrorResponse.FieldError> details = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toFieldError)
            .toList();

        AppLogger.warn(GlobalExceptionHandler.class, "Validation failed errors={}", details.size());

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Request validation failed",
            request,
            details
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
        List<ErrorResponse.FieldError> details = exception.getConstraintViolations()
            .stream()
            .map(violation -> new ErrorResponse.FieldError(
                sanitizeFieldName(violation.getPropertyPath().toString()),
                violation.getMessage()
            ))
            .toList();

        AppLogger.warn(GlobalExceptionHandler.class, "Constraint validation failed errors={}", details.size());

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Request validation failed",
            request,
            details
        );
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        AppLogger.warn(GlobalExceptionHandler.class, "Bad request handled: {}", exception.getClass().getSimpleName());
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "BAD_REQUEST",
            "Invalid request",
            request,
            List.of()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(
        MissingServletRequestParameterException exception,
        HttpServletRequest request
    ) {
        String parameterName = exception.getParameterName();
        AppLogger.warn(GlobalExceptionHandler.class, "Missing required parameter: {}", parameterName);

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "MISSING_REQUIRED_PARAMETER",
            "Required parameter '" + parameterName + "' is missing",
            request,
            List.of(new ErrorResponse.FieldError(parameterName, "Parameter is required"))
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(
        MissingRequestHeaderException exception,
        HttpServletRequest request
    ) {
        String headerName = exception.getHeaderName();
        AppLogger.warn(GlobalExceptionHandler.class, "Missing required header: {}", headerName);

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "MISSING_REQUIRED_HEADER",
            "Required header '" + headerName + "' is missing",
            request,
            List.of(new ErrorResponse.FieldError(headerName, "Header is required"))
        );
    }

    @ExceptionHandler({
        NoHandlerFoundException.class,
        NoResourceFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(Exception exception, HttpServletRequest request) {
        AppLogger.warn(GlobalExceptionHandler.class, "Resource not found: {}", request.getRequestURI());
        return buildResponse(
            HttpStatus.NOT_FOUND,
            "NOT_FOUND",
            "Resource not found",
            request,
            List.of()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
        HttpRequestMethodNotSupportedException exception,
        HttpServletRequest request
    ) {
        AppLogger.warn(GlobalExceptionHandler.class, "Method not allowed: {}", exception.getMethod());
        return buildResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            "METHOD_NOT_ALLOWED",
            "Method not allowed",
            request,
            List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        AppLogger.error(GlobalExceptionHandler.class, "Unexpected error", exception);
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "Unexpected error",
            request,
            List.of()
        );
    }

    private ErrorResponse.FieldError toFieldError(FieldError fieldError) {
        return new ErrorResponse.FieldError(sanitizeFieldName(fieldError.getField()), fieldError.getDefaultMessage());
    }

    private String sanitizeFieldName(String fieldName) {
        int dotIndex = fieldName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < fieldName.length() - 1) {
            return toPublicFieldName(fieldName.substring(dotIndex + 1));
        }
        return toPublicFieldName(fieldName);
    }

    private String toPublicFieldName(String fieldName) {
        return switch (fieldName) {
            case "xDeviceIp" -> "x-device-ip";
            case "xSession" -> "x-session";
            case "xUserid" -> "x-userid";
            default -> fieldName;
        };
    }

    private ResponseEntity<ErrorResponse> buildResponse(
        HttpStatus status,
        String code,
        String message,
        HttpServletRequest request,
        List<ErrorResponse.FieldError> details
    ) {
        ErrorResponse response = new ErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            code,
            message,
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(status).body(response);
    }
}
