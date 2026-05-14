package com.safeoutput.log4j2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.safeoutput.core.MaskRule;
import com.safeoutput.core.MaskRuleMatcher;
import com.safeoutput.core.MaskStrategyRegistry;

import java.util.Arrays;

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
    void explicitIdCardKeyValueMasksEvenWhenCheckCodeIsNotValid() {
        String masked = masker.mask("idCard=350102199001011234 invalid=110105199902300029");

        assertEquals("idCard=350102********1234 invalid=110105199902300029", masked);
    }

    @Test
    void unknownKeyValueTypeSkipsWithoutDefaultFallback() {
        SafeOutputLogMessageMasker custom = new SafeOutputLogMessageMasker(
                MaskRuleMatcher.withConfiguredRules(Arrays.asList(MaskRule.configured("custom")
                        .keys(Arrays.asList("customToken"))
                        .type("mobileM")
                        .build())),
                MaskStrategyRegistry.withBuiltIns());

        assertEquals("customToken=abcdef123456", custom.mask("customToken=abcdef123456"));
    }

    @Test
    void idCardFallbackCheckCodeCanBeDisabledWithoutSkippingDateValidation() {
        SafeOutputLogMessageMasker relaxed = new SafeOutputLogMessageMasker(1000, 300, true, false);

        String masked = relaxed.mask("candidate 350102199001011234 invalidDate 110105199902300029");

        assertEquals("candidate 350102********1234 invalidDate 110105199902300029", masked);
    }

    @Test
    void skipsLongMessagesAndLongValues() {
        SafeOutputLogMessageMasker shortMessageLimit = new SafeOutputLogMessageMasker(10, 100, true);
        SafeOutputLogMessageMasker shortValueLimit = new SafeOutputLogMessageMasker(1000, 5, false);

        assertEquals("mobile=13812345678", shortMessageLimit.mask("mobile=13812345678"));
        assertEquals("mobile=13812345678", shortValueLimit.mask("mobile=13812345678"));
    }
}
