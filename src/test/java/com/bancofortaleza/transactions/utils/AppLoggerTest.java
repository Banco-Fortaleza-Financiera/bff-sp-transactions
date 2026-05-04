package com.bancofortaleza.transactions.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AppLoggerTest {

    @AfterEach
    void tearDown() {
        AppLogger.clearContext();
    }

    @Test
    void putContextShouldStoreOnlyMeaningfulValuesInMdc() {
        // Arrange / Act
        AppLogger.putContext(AppLogger.REQUEST_ID, "request-1");
        AppLogger.putContext(AppLogger.DEVICE_IP, " ");
        AppLogger.putContext(Map.of(AppLogger.METHOD, "GET", AppLogger.PATH, "/transactions"));

        // Assert
        assertThat(AppLogger.getContext(AppLogger.REQUEST_ID)).isEqualTo("request-1");
        assertThat(AppLogger.getContext(AppLogger.DEVICE_IP)).isNull();
        assertThat(AppLogger.getContext(AppLogger.METHOD)).isEqualTo("GET");
        assertThat(AppLogger.getContext(AppLogger.PATH)).isEqualTo("/transactions");
    }

    @Test
    void loggingHelpersShouldExecuteWithoutThrowing() {
        // Arrange / Act / Assert
        AppLogger.info(AppLoggerTest.class, "info {}", "message");
        AppLogger.warn(AppLoggerTest.class, "warn {}", "message");
        AppLogger.error(AppLoggerTest.class, "error {}", "message");
        AppLogger.error(AppLoggerTest.class, "error with throwable", new IllegalStateException("boom"));
        AppLogger.requestStarted(AppLoggerTest.class, "GET", "/transactions");
        AppLogger.requestCompleted(AppLoggerTest.class, "GET", "/transactions", 200, 10L);
        AppLogger.requestFailed(AppLoggerTest.class, "GET", "/transactions", 10L, new RuntimeException("boom"));

        assertThat(AppLogger.getContext(AppLogger.REQUEST_ID)).isNull();
    }
}
