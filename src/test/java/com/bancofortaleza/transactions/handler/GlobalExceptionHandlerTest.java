package com.bancofortaleza.transactions.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bancofortaleza.transactions.domain.exceptions.ApiException;
import com.bancofortaleza.transactions.domain.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleApiExceptionShouldReturnConfiguredStatusAndCode() {
        // Arrange
        HttpServletRequest request = request("/transactions/99");
        ApiException exception = new ApiException(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "Not found");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleApiException(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("TRANSACTION_NOT_FOUND");
        assertThat(response.getBody().path()).isEqualTo("/transactions/99");
    }

    @Test
    void handleValidationShouldReturnPublicFieldNames() throws Exception {
        // Arrange
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "xDeviceIp", "must not be blank"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter(), bindingResult);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleValidation(exception, request("/transactions"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().details())
            .containsExactly(new ErrorResponse.FieldError("x-device-ip", "must not be blank"));
    }

    @Test
    void handleConstraintViolationShouldSanitizeNestedPropertyPath() {
        // Arrange
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("listTransactions.xSession");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(exception, request("/transactions"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().details())
            .containsExactly(new ErrorResponse.FieldError("x-session", "must not be blank"));
    }

    @Test
    void handleBadRequestShouldReturnGenericBadRequestResponse() throws Exception {
        // Arrange
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
            "abc",
            Integer.class,
            "id",
            methodParameter(),
            new IllegalArgumentException("invalid")
        );

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(exception, request("/transactions"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("BAD_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("Invalid request");
    }

    @Test
    void handleMissingRequestParameterShouldReturnParameterDetail() {
        // Arrange
        MissingServletRequestParameterException exception = new MissingServletRequestParameterException("startDate", "LocalDate");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleMissingRequestParameter(exception, request("/statement"));

        // Assert
        assertThat(response.getBody().code()).isEqualTo("MISSING_REQUIRED_PARAMETER");
        assertThat(response.getBody().details())
            .containsExactly(new ErrorResponse.FieldError("startDate", "Parameter is required"));
    }

    @Test
    void handleMissingRequestHeaderShouldReturnHeaderDetail() throws Exception {
        // Arrange
        MissingRequestHeaderException exception = new MissingRequestHeaderException("x-session", methodParameter());

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleMissingRequestHeader(exception, request("/transactions"));

        // Assert
        assertThat(response.getBody().code()).isEqualTo("MISSING_REQUIRED_HEADER");
        assertThat(response.getBody().details())
            .containsExactly(new ErrorResponse.FieldError("x-session", "Header is required"));
    }

    @Test
    void handleNotFoundShouldReturnNotFoundResponse() {
        // Arrange
        NoHandlerFoundException exception = new NoHandlerFoundException("GET", "/missing", new HttpHeaders());

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception, request("/missing"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void handleMethodNotAllowedShouldReturnMethodNotAllowedResponse() {
        // Arrange
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("PATCH", List.of("GET"));

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleMethodNotAllowed(exception, request("/transactions"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().code()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    void handleUnexpectedShouldReturnInternalServerErrorResponse() {
        // Act
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
            new IllegalStateException("boom"),
            request("/transactions")
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    private static HttpServletRequest request(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }

    private static MethodParameter methodParameter() throws Exception {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyEndpoint", String.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private static void dummyEndpoint(String value) {
    }
}
