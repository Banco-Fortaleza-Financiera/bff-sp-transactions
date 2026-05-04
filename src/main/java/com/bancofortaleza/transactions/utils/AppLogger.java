package com.bancofortaleza.transactions.utils;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public final class AppLogger {

    public static final String REQUEST_ID = "requestId";
    public static final String METHOD = "method";
    public static final String PATH = "path";
    public static final String DEVICE_IP = "deviceIp";

    private AppLogger() {
    }

    public static void info(Class<?> source, String message, Object... args) {
        logger(source).info(message, args);
    }

    public static void warn(Class<?> source, String message, Object... args) {
        logger(source).warn(message, args);
    }

    public static void error(Class<?> source, String message, Object... args) {
        logger(source).error(message, args);
    }

    public static void error(Class<?> source, String message, Throwable throwable) {
        logger(source).error(message, throwable);
    }

    public static void putContext(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    public static void putContext(Map<String, String> values) {
        values.forEach(AppLogger::putContext);
    }

    public static String getContext(String key) {
        return MDC.get(key);
    }

    public static void clearContext() {
        MDC.clear();
    }

    public static void requestStarted(Class<?> source, String method, String path) {
        info(source, "Request started");
    }

    public static void requestCompleted(Class<?> source, String method, String path, int status, long durationMillis) {
        info(source, "Request completed status={} durationMs={}", status, durationMillis);
    }

    public static void requestFailed(Class<?> source, String method, String path, long durationMillis, Throwable throwable) {
        error(source, "Request failed durationMs=" + durationMillis, throwable);
    }

    private static Logger logger(Class<?> source) {
        return LoggerFactory.getLogger(source);
    }
}
