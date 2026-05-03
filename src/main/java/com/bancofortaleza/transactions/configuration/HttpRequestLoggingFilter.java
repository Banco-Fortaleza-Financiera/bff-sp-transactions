package com.bancofortaleza.transactions.configuration;

import com.bancofortaleza.transactions.utils.AppLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "x-request-id";
    private static final String DEVICE_IP_HEADER = "x-device-ip";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();

        AppLogger.putContext(AppLogger.REQUEST_ID, resolveRequestId(request));
        AppLogger.putContext(AppLogger.METHOD, method);
        AppLogger.putContext(AppLogger.PATH, path);
        AppLogger.putContext(AppLogger.DEVICE_IP, resolveDeviceIp(request));

        response.setHeader(REQUEST_ID_HEADER, AppLogger.getContext(AppLogger.REQUEST_ID));
        AppLogger.requestStarted(HttpRequestLoggingFilter.class, method, path);

        try {
            filterChain.doFilter(request, response);
            AppLogger.requestCompleted(
                HttpRequestLoggingFilter.class,
                method,
                path,
                response.getStatus(),
                elapsedMillis(startTime)
            );
        } catch (Exception exception) {
            AppLogger.requestFailed(HttpRequestLoggingFilter.class, method, path, elapsedMillis(startTime), exception);
            throw exception;
        } finally {
            AppLogger.clearContext();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId;
    }

    private String resolveDeviceIp(HttpServletRequest request) {
        String deviceIp = request.getHeader(DEVICE_IP_HEADER);
        if (deviceIp == null || deviceIp.isBlank()) {
            return request.getRemoteAddr();
        }
        return deviceIp;
    }

    private long elapsedMillis(long startTime) {
        return System.currentTimeMillis() - startTime;
    }
}
