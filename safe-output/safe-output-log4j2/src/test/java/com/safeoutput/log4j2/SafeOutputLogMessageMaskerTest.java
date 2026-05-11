package com.safeoutput.log4j2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class SafeOutputLogMessageMaskerTest {

    private final SafeOutputLogMessageMasker masker = new SafeOutputLogMessageMasker();

    @Test
    void masksJsonLikeSensitiveKeyValueFragments() {
        String masked = masker.mask("{\"mobile\":\"13812345678\",\"email\":\"foo@example.com\","
                + "\"idCard\":\"11010519491231002X\",\"bankCard\":\"6222021234567890123\"}");

        assertFalse(masked.contains("13812345678"));
        assertFalse(masked.contains("foo@example.com"));
        assertFalse(masked.contains("11010519491231002X"));
        assertFalse(masked.contains("6222021234567890123"));
        assertEquals("{\"mobile\":\"138****5678\",\"email\":\"foo****@example.com\","
                + "\"idCard\":\"110105********002X\",\"bankCard\":\"622202*********0123\"}", masked);
    }

    @Test
    void malformedAndPlainTextMessagesStayPrintable() {
        assertEquals("{\"mobile\":", masker.mask("{\"mobile\":"));
        assertEquals("plain text without key values", masker.mask("plain text without key values"));
    }
}
