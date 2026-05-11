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

    private static void assertMatch(Optional<RuleMatch> match, MaskType type, String ruleName, RuleSource source) {
        assertEquals(type, match.get().getMaskType());
        assertEquals(ruleName, match.get().getRuleName());
        assertEquals(source, match.get().getSource());
    }
}
