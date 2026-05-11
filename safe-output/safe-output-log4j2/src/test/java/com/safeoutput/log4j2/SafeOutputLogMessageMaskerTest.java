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

    @Test
    void regexFallbackMasksMobileEmailAndStrictIdCardOnly() {
        String masked = masker.mask("contact 13812345678 foo@example.com id 11010519491231002X"
                + " invalid 110105194912310021 flow 123456789012345678 bank 6222021234567890123");

        assertFalse(masked.contains("13812345678"));
        assertFalse(masked.contains("foo@example.com"));
        assertFalse(masked.contains("11010519491231002X"));
        assertEquals("contact 138****5678 foo****@example.com id 110105********002X"
                + " invalid 110105194912310021 flow 123456789012345678 bank 6222021234567890123", masked);
    }

    @Test
    void skipsLongMessagesAndLongValues() {
        SafeOutputLogMessageMasker shortMessageLimit = new SafeOutputLogMessageMasker(10, 100, true);
        SafeOutputLogMessageMasker shortValueLimit = new SafeOutputLogMessageMasker(1000, 5, false);

        assertEquals("mobile=13812345678", shortMessageLimit.mask("mobile=13812345678"));
        assertEquals("mobile=13812345678", shortValueLimit.mask("mobile=13812345678"));
    }
}
