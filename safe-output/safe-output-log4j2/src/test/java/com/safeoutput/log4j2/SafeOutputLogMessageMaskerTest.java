package com.safeoutput.log4j2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.safeoutput.core.MaskRule;
import com.safeoutput.core.MaskRuleMatcher;
import com.safeoutput.core.MaskContext;
import com.safeoutput.core.MaskScene;
import com.safeoutput.core.InMemoryLogRuleSuggestionCollector;
import com.safeoutput.core.LogRuleSuggestionMetric;
import com.safeoutput.core.MaskStrategy;
import com.safeoutput.core.MaskStrategyRegistry;
import com.safeoutput.core.MaskTypes;
import com.safeoutput.core.UnknownTypeRecorder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    void regexFallbackCollectsNearbyKeySuggestionsWithoutRawValues() {
        InMemoryLogRuleSuggestionCollector collector = new InMemoryLogRuleSuggestionCollector();
        SafeOutputLogMessageMasker custom = new SafeOutputLogMessageMasker(MaskRuleMatcher.withDefaultRules(),
                MaskStrategyRegistry.withBuiltIns(), collector);

        custom.mask("phoneNo=13812345678 certNum: 11010519491231002X mailAddr=foo@example.com");
        custom.mask("phoneNo=13912345678");

        List<LogRuleSuggestionMetric> metrics = collector.snapshotSuggestions();
        assertEquals(3, metrics.size());
        assertMetric(metrics, "phoneno", MaskTypes.MOBILE, 2);
        assertMetric(metrics, "certnum", MaskTypes.ID_CARD, 1);
        assertMetric(metrics, "mailaddr", MaskTypes.EMAIL, 1);
        assertFalse(metrics.toString().contains("13812345678"));
        assertFalse(metrics.toString().contains("foo@example.com"));
        assertFalse(metrics.toString().contains("11010519491231002X"));
    }

    @Test
    void regexFallbackSkipsConfiguredNearbyKeysForSuggestions() {
        InMemoryLogRuleSuggestionCollector collector = new InMemoryLogRuleSuggestionCollector();
        SafeOutputLogMessageMasker custom = new SafeOutputLogMessageMasker(
                MaskRuleMatcher.withConfiguredRules(Arrays.asList(MaskRule.configured("phone")
                        .keys(Arrays.asList("phoneNo"))
                        .type(MaskTypes.MOBILE)
                        .build())),
                MaskStrategyRegistry.withBuiltIns(), collector);

        custom.mask("phoneNo=13812345678 mailAddr=foo@example.com");

        List<LogRuleSuggestionMetric> metrics = collector.snapshotSuggestions();
        assertEquals(1, metrics.size());
        assertEquals("mailaddr", metrics.get(0).getKey());
    }

    @Test
    void explicitIdCardKeyValueMasksEvenWhenCheckCodeIsNotValid() {
        String masked = masker.mask("idCard=350102199001011234 invalid=110105199902300029");

        assertEquals("idCard=350102********1234 invalid=110105199902300029", masked);
    }

    @Test
    void unknownKeyValueTypeFallsBackToDefault() {
        AtomicInteger unknownCount = new AtomicInteger();
        SafeOutputLogMessageMasker custom = new SafeOutputLogMessageMasker(
                MaskRuleMatcher.withConfiguredRules(Arrays.asList(MaskRule.configured("custom")
                        .keys(Arrays.asList("customToken"))
                        .type("mobileM")
                        .build())),
                MaskStrategyRegistry.withBuiltIns(), new UnknownTypeRecorder() {
                    @Override
                    public void recordUnknownType(String type, MaskScene scene) {
                        if ("mobilem".equals(type) && scene == MaskScene.LOG) {
                            unknownCount.incrementAndGet();
                        }
                    }
                });

        assertEquals("customToken=****", custom.mask("customToken=abcdef123456"));
        assertEquals(1, unknownCount.get());
    }

    @Test
    void configuredKeyValueRulesMaskSupportedFormatsAndNames() {
        SafeOutputLogMessageMasker custom = new SafeOutputLogMessageMasker(
                MaskRuleMatcher.withConfiguredRules(Arrays.asList(MaskRule.configured("name")
                        .keys(Arrays.asList("chineseName"))
                        .type(MaskTypes.CHINESE_NAME)
                        .build())),
                MaskStrategyRegistry.withBuiltIns());

        assertEquals("chineseName: 张*", custom.mask("chineseName: 张三"));
        assertEquals("\"chineseName\":\"王*明\"", custom.mask("\"chineseName\":\"王小明\""));
        assertEquals("chineseName = '李*'", custom.mask("chineseName = '李雷'"));
        assertEquals("'chineseName' : 赵*", custom.mask("'chineseName' : 赵六"));
    }

    @Test
    void customTypeMasksWhenStrategyIsRegistered() {
        SafeOutputLogMessageMasker custom = new SafeOutputLogMessageMasker(
                MaskRuleMatcher.withConfiguredRules(Arrays.asList(MaskRule.configured("customMobile")
                        .keys(Arrays.asList("mobileM"))
                        .type("mobileM")
                        .build())),
                MaskStrategyRegistry.withBuiltIns(Arrays.asList(new MaskStrategy() {
                    @Override
                    public String type() {
                        return "mobileM";
                    }

                    @Override
                    public String mask(String rawValue, MaskContext context) {
                        return rawValue.substring(0, 3) + "****" + rawValue.substring(7);
                    }
                })));

        assertEquals("mobileM=138****5678", custom.mask("mobileM=13812345678"));
    }

    @Test
    void ignoreKeysWinOverConfiguredLogRulesAndPathsAreNotUsed() {
        SafeOutputLogMessageMasker custom = new SafeOutputLogMessageMasker(
                MaskRuleMatcher.builder()
                        .configuredRules(Arrays.asList(
                                MaskRule.configured("ignoredName")
                                        .keys(Arrays.asList("chineseName"))
                                        .type(MaskTypes.CHINESE_NAME)
                                        .build(),
                                MaskRule.configured("pathOnly")
                                        .paths(Arrays.asList("$.chineseName"))
                                        .type(MaskTypes.CHINESE_NAME)
                                        .build()))
                        .ignoreKeys(Collections.singletonList("chineseName"))
                        .build(),
                MaskStrategyRegistry.withBuiltIns());

        assertEquals("chineseName=张三 other=王五", custom.mask("chineseName=张三 other=王五"));
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

    @Test
    void keyValueRuleSwitchAndRuleKeyLimitSkipKeyValueMasking() {
        SafeOutputLogMessageMasker disabled = new SafeOutputLogMessageMasker(
                MaskRuleMatcher.withDefaultRules(), MaskStrategyRegistry.withBuiltIns(), 1000, 100, false, false, 100);
        SafeOutputLogMessageMasker limited = new SafeOutputLogMessageMasker(
                MaskRuleMatcher.withDefaultRules(), MaskStrategyRegistry.withBuiltIns(), 1000, 100, false, true, 1);

        assertEquals("mobile=13812345678", disabled.mask("mobile=13812345678"));
        assertEquals("mobile=13812345678", limited.mask("mobile=13812345678"));
    }

    @Test
    void strategyExceptionReturnsOriginalLogMessage() {
        SafeOutputLogMessageMasker custom = new SafeOutputLogMessageMasker(
                MaskRuleMatcher.withConfiguredRules(Arrays.asList(MaskRule.configured("broken")
                        .keys(Arrays.asList("broken"))
                        .type("broken")
                        .build())),
                MaskStrategyRegistry.withBuiltIns(Arrays.asList(new MaskStrategy() {
                    @Override
                    public String type() {
                        return "broken";
                    }

                    @Override
                    public String mask(String rawValue, MaskContext context) {
                        throw new IllegalStateException("boom");
                    }
                })), 1000, 100, false, true, 100);

        assertEquals("broken=secret", custom.mask("broken=secret"));
    }

    private static void assertMetric(List<LogRuleSuggestionMetric> metrics, String key, String type, long hitCount) {
        for (LogRuleSuggestionMetric metric : metrics) {
            if (key.equals(metric.getKey()) && type.equals(metric.getType())) {
                assertEquals(hitCount, metric.getHitCount());
                assertEquals(key + "=<" + type + ">", metric.getEvidence());
                return;
            }
        }
        throw new AssertionError("Missing metric " + key + " " + type);
    }
}
