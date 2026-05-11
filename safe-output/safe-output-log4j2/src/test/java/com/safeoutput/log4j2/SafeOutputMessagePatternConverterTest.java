package com.safeoutput.log4j2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

class SafeOutputMessagePatternConverterTest {

    @Test
    void log4j2DiscoversConverterAndMasksMessage() {
        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%safeOutputMsg")
                .withAlwaysWriteExceptions(false)
                .build();

        String formatted = layout.toSerializable(event("mobile=13800138000 password=abc"));

        assertTrue(formatted.contains("mobile=138****8000"));
        assertTrue(formatted.contains("password=********"));
        assertFalse(formatted.contains("13800138000"));
        assertFalse(formatted.contains("password=abc"));
    }

    @Test
    void disabledConverterKeepsOriginalMessage() {
        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%safeOutputMsg{enabled=false}")
                .withAlwaysWriteExceptions(false)
                .build();

        String formatted = layout.toSerializable(event("mobile=13800138000"));

        assertTrue(formatted.contains("mobile=13800138000"));
    }

    @Test
    void converterOptionsApplyLengthLimits() {
        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%safeOutputMsg{maxMessageLength=10,maxValueLength=100}")
                .withAlwaysWriteExceptions(false)
                .build();

        String formatted = layout.toSerializable(event("mobile=13800138000"));

        assertTrue(formatted.contains("mobile=13800138000"));
    }

    private static LogEvent event(String message) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName("safe-output-test")
                .setMessage(new SimpleMessage(message))
                .build();
    }
}
