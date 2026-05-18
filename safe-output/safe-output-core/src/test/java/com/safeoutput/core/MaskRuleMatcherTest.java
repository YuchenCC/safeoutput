package com.safeoutput.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class MaskRuleMatcherTest {

    @Test
    void defaultRuleLibraryIsTheSingleSourceForBuiltInFieldRules() {
        List<MaskRule> rules = DefaultMaskRules.all();

        assertEquals(5, rules.size());
        assertEquals("default.mobile", rules.get(0).getName());
        assertEquals(Arrays.asList("mobile", "phone", "telephone", "tel", "userMobile"), rules.get(0).getKeys());
        assertEquals(MaskTypes.MOBILE, rules.get(0).getType());
        assertEquals("default.id-card", rules.get(1).getName());
        assertEquals(Arrays.asList("idCard", "certNo", "identityNo", "certificateNo"), rules.get(1).getKeys());
        assertEquals(MaskTypes.ID_CARD, rules.get(1).getType());
        assertEquals("default.bank-card", rules.get(2).getName());
        assertEquals(Arrays.asList("bankCard", "cardNo", "bankNo"), rules.get(2).getKeys());
        assertEquals(MaskTypes.BANK_CARD, rules.get(2).getType());
        assertEquals("default.email", rules.get(3).getName());
        assertEquals(Arrays.asList("email", "mail"), rules.get(3).getKeys());
        assertEquals(MaskTypes.EMAIL, rules.get(3).getType());
        assertEquals("default.password", rules.get(4).getName());
        assertEquals(Arrays.asList("password", "secret", "token"), rules.get(4).getKeys());
        assertEquals(MaskTypes.PASSWORD, rules.get(4).getType());
    }

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
    void pathRulesSupportAnyNumericCollectionIndexWildcard() {
        MaskRuleMatcher matcher = MaskRuleMatcher.withConfiguredRules(Arrays.asList(
                MaskRule.configured("itemTitle")
                        .paths(Arrays.asList("$.items[*].title"))
                        .type(MaskType.DEFAULT)
                        .build()));

        assertMatch(matcher.match("title", "$.items[0].title"), MaskType.DEFAULT, "itemTitle",
                RuleSource.CONFIGURED);
        assertMatch(matcher.match("title", "$.items[12].title"), MaskType.DEFAULT, "itemTitle",
                RuleSource.CONFIGURED);

        assertFalse(matcher.match("title", "$.items.title").isPresent());
        assertFalse(matcher.match("title", "$.orders[0].title").isPresent());
        assertFalse(matcher.match("title", "$.items[abc].title").isPresent());
    }

    @Test
    void defaultRulesCanBeDisabledWithoutDisablingConfiguredRules() {
        MaskRuleMatcher matcher = MaskRuleMatcher.builder()
                .defaultRulesEnabled(false)
                .configuredRules(Arrays.asList(
                        MaskRule.configured("realName")
                                .keys(Arrays.asList("realName"))
                                .type(MaskType.CHINESE_NAME)
                                .build(),
                        MaskRule.configured("profileMobile")
                                .paths(Arrays.asList("$.profile.mobile"))
                                .type(MaskType.MOBILE)
                                .build()))
                .build();

        assertFalse(matcher.match("mobile", "$.mobile").isPresent());
        assertFalse(matcher.match("email", "$.email").isPresent());
        assertFalse(matcher.match("password", "$.password").isPresent());
        assertMatch(matcher.match("realName", "$.realName"), MaskType.CHINESE_NAME, "realName",
                RuleSource.CONFIGURED);
        assertMatch(matcher.match("mobile", "$.profile.mobile"), MaskType.MOBILE, "profileMobile",
                RuleSource.CONFIGURED);
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

        MaskRuleMatcher wildcardIgnoreMatcher = MaskRuleMatcher.builder()
                .ignorePaths(Arrays.asList("$.items[*].title"))
                .build();
        RuleMatch wildcardFieldIgnored = wildcardIgnoreMatcher.decide(MaskRuleRequest.builder()
                .key("title")
                .path("$.items[0].title")
                .annotationType(MaskType.DEFAULT)
                .build()).get();
        assertEquals(RuleAction.IGNORE, wildcardFieldIgnored.getAction());
        assertEquals(RuleSource.FIELD_IGNORE, wildcardFieldIgnored.getSource());

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

    @Test
    void logKeyMatchesAreBuiltOnceWithIgnoreAndKeyLimit() {
        MaskRuleMatcher matcher = MaskRuleMatcher.builder()
                .ignoreKeys(Arrays.asList("email"))
                .configuredRules(Arrays.asList(MaskRule.configured("realName")
                        .keys(Arrays.asList("realName"))
                        .type(MaskType.CHINESE_NAME)
                        .build()))
                .build();

        Map<String, RuleMatch> matches = matcher.logKeyMatches(20);

        assertMatch(Optional.of(matches.get("realname")), MaskType.CHINESE_NAME, "realName", RuleSource.CONFIGURED);
        assertEquals(RuleAction.IGNORE, matches.get("email").getAction());
        assertFalse(matcher.logKeyMatches(1).containsKey("realname"));
        assertFalse(matches.containsKey("$.realName"));
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
