package com.safeoutput.spring.boot.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.safeoutput.core.MaskRuleMatcher;
import com.safeoutput.core.MaskTypes;
import com.safeoutput.core.RuleSource;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputAutoConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SafeOutputRuleMatcherAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SafeOutputAutoConfiguration.class);

    @Test
    void autoConfigurationBuildsRuleMatcherFromProperties() {
        contextRunner
                .withPropertyValues(
                        "safe-output.rules[0].name=realName",
                        "safe-output.rules[0].keys[0]=realName",
                        "safe-output.rules[0].type=CHINESE_NAME")
                .run(context -> {
                    MaskRuleMatcher matcher = context.getBean(MaskRuleMatcher.class);

                    assertEquals(MaskTypes.CHINESE_NAME, matcher.match("realName", "$.realName").get().getMaskType());
                    assertEquals(RuleSource.CONFIGURED, matcher.match("realName", "$.realName").get().getSource());
                    assertEquals(MaskTypes.MOBILE, matcher.match("mobile", "$.mobile").get().getMaskType());
                    assertEquals(RuleSource.DEFAULT, matcher.match("mobile", "$.mobile").get().getSource());
                });
    }
}
