package com.safeoutput.spring.boot.starter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

class SafeOutputLog4j2StarterIntegrationTest {

    @Test
    void starterClasspathExposesSafeOutputMessageConverter() {
        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%safeOutputMsg")
                .withAlwaysWriteExceptions(false)
                .build();

        String formatted = layout.toSerializable(event("email=zhangsan@example.com"));

        assertTrue(formatted.contains("email=zha****@example.com"));
        assertFalse(formatted.contains("zhangsan@example.com"));
    }

    private static LogEvent event(String message) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName("safe-output-starter-test")
                .setMessage(new SimpleMessage(message))
                .build();
    }
}
