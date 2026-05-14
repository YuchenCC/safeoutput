package com.safeoutput.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class MaskRuleMatcherTest {

    @Test
    void defaultRulesMatchClearSensitiveKeysAndSkipAmbiguousKeys() {
        MaskRuleMatcher matcher = MaskRuleMatcher.withDefaultRules();

        assertMatch(matcher.match("mobile", "$.mobile"), MaskType.MOBILE, "default.mobile", RuleSource.DEFAULT);
        assertMatch(matcher.match("phone", "$.phone"), MaskType.MOBILE, "default.mobile", RuleSource.DEFAULT);
        assertMatch(matcher.match("idCard", "$.idCard"), MaskType.ID_CARD, "default.id-card", RuleSource.DEFAULT);
        assertMatch(matcher.match("certNo", "$.certNo"), MaskType.ID_CARD, "default.id-card", RuleSource.DEFAULT);
        assertMatch(matcher.match("bankCard", "$.bankCard"), MaskType.BANK_CARD, "default.bank-card",
                RuleSource.DEFAULT);
        assertMatch(matcher.match("email", "$.email"), MaskType.EMAIL, "default.email", RuleSource.DEFAULT);
        assertMatch(matcher.match("password", "$.password"), MaskType.PASSWORD, "default.password",
                RuleSource.DEFAULT);
        assertMatch(matcher.match("secret", "$.secret"), MaskType.PASSWORD, "default.password", RuleSource.DEFAULT);
        assertMatch(matcher.match("token", "$.token"), MaskType.PASSWORD, "default.password", RuleSource.DEFAULT);

        assertFalse(matcher.match("name", "$.name").isPresent());
        assertFalse(matcher.match("id", "$.id").isPresent());
        assertFalse(matcher.match("account", "$.account").isPresent());
    }

    @Test
    void configuredRulesCanAddPathRulesOverrideDefaultsAndBeDisabled() {
        MaskRuleMatcher matcher = MaskRuleMatcher.withConfiguredRules(Arrays.asList(
                MaskRule.configured("realName")
                        .keys(Arrays.asList("realName"))
                        .type(MaskType.CHINESE_NAME)
                        .build(),
                MaskRule.configured("mobileAsDefault")
                        .keys(Arrays.asList("mobile"))
                        .type(MaskType.DEFAULT)
                        .build(),
                MaskRule.configured("userNamePath")
                        .paths(Arrays.asList("$.user.name"))
                        .type(MaskType.CHINESE_NAME)
                        .build(),
                MaskRule.configured("disabledEmail")
                        .keys(Arrays.asList("email"))
                        .type(MaskType.DEFAULT)
                        .enabled(false)
                        .build()));

        assertMatch(matcher.match("realName", "$.realName"), MaskType.CHINESE_NAME, "realName",
                RuleSource.CONFIGURED);
        assertMatch(matcher.match("mobile", "$.mobile"), MaskType.DEFAULT, "mobileAsDefault",
                RuleSource.CONFIGURED);
        assertMatch(matcher.match("name", "$.user.name"), MaskType.CHINESE_NAME, "userNamePath",
                RuleSource.CONFIGURED);
        assertMatch(matcher.match("email", "$.email"), MaskType.EMAIL, "default.email", RuleSource.DEFAULT);
    }

    @Test
    void decisionAppliesFixedPrecedenceAcrossIgnoreAnnotationRulesDefaultAndFallback() {
        MaskRuleMatcher matcher = MaskRuleMatcher.builder()
                .ignoreKeys(Arrays.asList("email"))
                .ignorePaths(Arrays.asList("$.ignored.mobile"))
                .configuredRules(Arrays.asList(
                        MaskRule.configured("pathMobile")
                                .paths(Arrays.asList("$.user.mobile"))
                                .type(MaskType.MOBILE)
                                .build(),
                        MaskRule.configured("keyEmail")
                                .keys(Arrays.asList("email"))
                                .type(MaskType.EMAIL)
                                .build()))
                .build();

        RuleMatch apiIgnored = matcher.decide(MaskRuleRequest.builder()
                .apiIgnored(true)
                .key("email")
                .path("$.ignored.mobile")
                .annotationType(MaskType.ID_CARD)
                .regexFallbackType(MaskType.DEFAULT)
                .build()).get();
        assertEquals(RuleAction.IGNORE, apiIgnored.getAction());
        assertEquals(RuleSource.API_IGNORE, apiIgnored.getSource());

        RuleMatch fieldIgnored = matcher.decide(MaskRuleRequest.builder()
                .key("email")
                .path("$.ignored.mobile")
                .annotationType(MaskType.ID_CARD)
                .regexFallbackType(MaskType.DEFAULT)
                .build()).get();
        assertEquals(RuleAction.IGNORE, fieldIgnored.getAction());
        assertEquals(RuleSource.FIELD_IGNORE, fieldIgnored.getSource());

        RuleMatch annotated = matcher.decide(MaskRuleRequest.builder()
                .key("plainName")
                .path("$.plainName")
                .annotationType(MaskType.CHINESE_NAME)
                .regexFallbackType(MaskType.DEFAULT)
                .build()).get();
        assertMatch(Optional.of(annotated), MaskType.CHINESE_NAME, "annotation", RuleSource.ANNOTATION);

        RuleMatch pathRule = matcher.decide(MaskRuleRequest.builder()
                .key("mobile")
                .path("$.user.mobile")
                .build()).get();
        assertMatch(Optional.of(pathRule), MaskType.MOBILE, "pathMobile", RuleSource.CONFIGURED);

        RuleMatch keyRule = matcher.decide(MaskRuleRequest.builder()
                .key("email")
                .path("$.contact")
                .build()).get();
        assertEquals(RuleAction.IGNORE, keyRule.getAction());
        assertEquals(RuleSource.FIELD_IGNORE, keyRule.getSource());

        RuleMatch defaultRule = matcher.decide(MaskRuleRequest.builder()
                .key("password")
                .path("$.credential")
                .build()).get();
        assertMatch(Optional.of(defaultRule), MaskType.PASSWORD, "default.password", RuleSource.DEFAULT);

        RuleMatch fallback = matcher.decide(MaskRuleRequest.builder()
                .key("message")
                .path("$.message")
                .regexFallbackType(MaskType.MOBILE)
                .build()).get();
        assertMatch(Optional.of(fallback), MaskType.MOBILE, "regex-fallback", RuleSource.REGEX_FALLBACK);
    }

    private static void assertMatch(Optional<RuleMatch> match, MaskType type, String ruleName, RuleSource source) {
        assertMatch(match, MaskTypes.from(type), ruleName, source);
    }

    private static void assertMatch(Optional<RuleMatch> match, String type, String ruleName, RuleSource source) {
        assertEquals(RuleAction.MASK, match.get().getAction());
        assertEquals(type, match.get().getMaskType());
        assertEquals(ruleName, match.get().getRuleName());
        assertEquals(source, match.get().getSource());
    }
}
