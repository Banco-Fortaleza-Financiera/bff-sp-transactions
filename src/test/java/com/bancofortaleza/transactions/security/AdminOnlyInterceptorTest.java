package com.bancofortaleza.transactions.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bancofortaleza.transactions.domain.exceptions.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class AdminOnlyInterceptorTest {

    private final AdminOnlyInterceptor interceptor = new AdminOnlyInterceptor();

    @Test
    void preHandleShouldAllowNonHandlerMethodWithoutHeader() {
        // Arrange / Act
        boolean result = interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), new Object());

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void preHandleShouldAllowPublicHandlerWithoutHeader() throws Exception {
        // Arrange
        HandlerMethod handlerMethod = handlerMethod("publicEndpoint");

        // Act
        boolean result = interceptor.preHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), handlerMethod);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void preHandleShouldAllowAdminHandlerWhenUserIdHeaderIsPositive() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("x-userid")).thenReturn("10");

        // Act
        boolean result = interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod("adminEndpoint"));

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void preHandleShouldDetectAdminAnnotationOnImplementationMethod() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("x-userid")).thenReturn("10");
        Method generatedMethod = GeneratedApi.class.getMethod("_generatedEndpoint");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), generatedMethod);

        // Act
        boolean result = interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void preHandleShouldRejectMissingUserIdHeaderForAdminHandlers() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("x-userid")).thenReturn(" ");

        // Act / Assert
        assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod("adminEndpoint")))
            .isInstanceOf(ApiException.class)
            .hasMessage("Required header 'x-userid' is missing")
            .extracting("code")
            .isEqualTo("MISSING_REQUIRED_HEADER");
    }

    @Test
    void preHandleShouldRejectInvalidUserIdHeaderForAdminHandlers() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("x-userid")).thenReturn("0");

        // Act / Assert
        assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod("adminEndpoint")))
            .isInstanceOf(ApiException.class)
            .hasMessage("Header 'x-userid' must be a positive integer")
            .extracting("code")
            .isEqualTo("INVALID_HEADER");
    }

    @Test
    void preHandleShouldRejectNonNumericUserIdHeaderForAdminHandlers() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("x-userid")).thenReturn("abc");

        // Act / Assert
        assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod("adminEndpoint")))
            .isInstanceOf(ApiException.class)
            .extracting("code")
            .isEqualTo("INVALID_HEADER");
    }

    private static HandlerMethod handlerMethod(String methodName) throws Exception {
        return new HandlerMethod(new TestController(), TestController.class.getMethod(methodName));
    }

    interface GeneratedApi {
        void _generatedEndpoint();
    }

    static class TestController implements GeneratedApi {

        public void publicEndpoint() {
        }

        @AdminOnly
        public void adminEndpoint() {
        }

        @AdminOnly
        public void generatedEndpoint() {
        }

        @Override
        public void _generatedEndpoint() {
        }
    }
}
