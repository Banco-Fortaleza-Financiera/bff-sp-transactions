package com.bancofortaleza.transactions.security;

import com.bancofortaleza.transactions.domain.exceptions.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminOnlyInterceptor implements HandlerInterceptor {

    private static final String X_USER_ID_HEADER = "x-userid";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod) || !requiresAdmin(handlerMethod)) {
            return true;
        }

        parseUserId(request.getHeader(X_USER_ID_HEADER));
        return true;
    }

    private boolean requiresAdmin(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(AdminOnly.class)
            || implementationMethodRequiresAdmin(handlerMethod);
    }

    private boolean implementationMethodRequiresAdmin(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        String methodName = method.getName();
        String implementationMethodName = methodName.startsWith("_") ? methodName.substring(1) : methodName;

        try {
            return handlerMethod.getBeanType()
                .getMethod(implementationMethodName, method.getParameterTypes())
                .isAnnotationPresent(AdminOnly.class);
        } catch (NoSuchMethodException exception) {
            return false;
        }
    }

    private Integer parseUserId(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "MISSING_REQUIRED_HEADER",
                "Required header '" + X_USER_ID_HEADER + "' is missing"
            );
        }

        try {
            int userId = Integer.parseInt(headerValue);
            if (userId < 1) {
                throw invalidUserIdHeaderException();
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw invalidUserIdHeaderException();
        }
    }

    private ApiException invalidUserIdHeaderException() {
        return new ApiException(
            HttpStatus.BAD_REQUEST,
            "INVALID_HEADER",
            "Header '" + X_USER_ID_HEADER + "' must be a positive integer"
        );
    }
}
