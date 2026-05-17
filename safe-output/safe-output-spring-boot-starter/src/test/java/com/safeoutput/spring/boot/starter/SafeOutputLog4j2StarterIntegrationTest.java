package com.safeoutput.spring.boot.starter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.safeoutput.core.MaskContext;
import com.safeoutput.core.MaskStrategy;
import com.safeoutput.log4j2.SafeOutputLog4j2Runtime;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputAutoConfiguration;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class SafeOutputLog4j2StarterIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SafeOutputAutoConfiguration.class);

    @BeforeEach
    @AfterEach
    void resetLog4j2Runtime() {
        SafeOutputLog4j2Runtime.reset();
    }

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

    @Test
    void springRulesDriveRealLog4j2Converter() {
        contextRunner
                .withPropertyValues(
                        "safe-output.rules[0].name=logName",
                        "safe-output.rules[0].keys[0]=chineseName",
                        "safe-output.rules[0].type=CHINESE_NAME")
                .run(context -> {
                    PatternLayout layout = safeOutputLayout();

                    String formatted = layout.toSerializable(event("chineseName: 张三"));

                    assertTrue(formatted.contains("chineseName: 张*"));
                    assertFalse(formatted.contains("张三"));
                });
    }

    @Test
    void springDefaultRulesCanBeDisabledForRealLog4j2Converter() {
        contextRunner
                .withPropertyValues("safe-output.rules.default-enabled=false")
                .run(context -> {
                    PatternLayout layout = safeOutputLayout();

                    String formatted = layout.toSerializable(event("mobile=13812345678 email=foo@example.com"));

                    assertTrue(formatted.contains("mobile=13812345678"));
                    assertTrue(formatted.contains("email=foo@example.com"));
                });
    }

    @Test
    void springCustomStrategyDrivesRealLog4j2Converter() {
        contextRunner
                .withUserConfiguration(CustomStrategyConfiguration.class)
                .withPropertyValues(
                        "safe-output.rules[0].name=customMobile",
                        "safe-output.rules[0].keys[0]=mobileM",
                        "safe-output.rules[0].type=mobileM")
                .run(context -> {
                    PatternLayout layout = safeOutputLayout();

                    String formatted = layout.toSerializable(event("mobileM=13812345678"));

                    assertTrue(formatted.contains("mobileM=mobileM-5678"));
                    assertFalse(formatted.contains("13812345678"));
                });
    }

    @Test
    void springIgnoreKeysWinAndRulePathsDoNotDriveRealLog4j2Converter() {
        contextRunner
                .withPropertyValues(
                        "safe-output.rules[0].name=ignoredName",
                        "safe-output.rules[0].keys[0]=chineseName",
                        "safe-output.rules[0].type=CHINESE_NAME",
                        "safe-output.rules[1].name=pathOnlyName",
                        "safe-output.rules[1].paths[0]=$.pathOnlyName",
                        "safe-output.rules[1].type=CHINESE_NAME",
                        "safe-output.ignore.keys[0]=chineseName")
                .run(context -> {
                    PatternLayout layout = safeOutputLayout();

                    String formatted = layout.toSerializable(event("chineseName=张三 pathOnlyName=李四"));

                    assertTrue(formatted.contains("chineseName=张三"));
                    assertTrue(formatted.contains("pathOnlyName=李四"));
                });
    }

    @Test
    void unknownSpringCustomTypeSkipsRealLog4j2ConverterValue() {
        contextRunner
                .withPropertyValues(
                        "safe-output.rules[0].name=customToken",
                        "safe-output.rules[0].keys[0]=customToken",
                        "safe-output.rules[0].type=mobileM")
                .run(context -> {
                    PatternLayout layout = safeOutputLayout();

                    String formatted = layout.toSerializable(event("customToken=abcdef123456"));

                    assertTrue(formatted.contains("customToken=abcdef123456"));
                });
    }

    @Test
    void longMessageWithinLimitMasksAllConfiguredKeyValuesAndFallbackValues() {
        contextRunner
                .withUserConfiguration(CustomStrategyConfiguration.class)
                .withPropertyValues(
                        "safe-output.log.max-message-length=1000",
                        "safe-output.log.regex-fallback.enabled=true",
                        "safe-output.rules[0].name=logName",
                        "safe-output.rules[0].keys[0]=chineseName",
                        "safe-output.rules[0].type=CHINESE_NAME",
                        "safe-output.rules[1].name=customMobile",
                        "safe-output.rules[1].keys[0]=mobileM",
                        "safe-output.rules[1].type=mobileM")
                .run(context -> {
                    PatternLayout layout = safeOutputLayout();

                    String formatted = layout.toSerializable(event("traceId=abc001 payload="
                            + repeat("segment-", 20)
                            + " chineseName=张三 mobileM=13812345678 otherName=李四"
                            + " contact=foo@example.com chineseName: 王小明"));

                    assertTrue(formatted.contains("chineseName=张*"));
                    assertTrue(formatted.contains("mobileM=mobileM-5678"));
                    assertTrue(formatted.contains("otherName=李四"));
                    assertTrue(formatted.contains("contact=foo****@example.com"));
                    assertTrue(formatted.contains("chineseName: 王*明"));
                    assertFalse(formatted.contains("张三"));
                    assertFalse(formatted.contains("13812345678"));
                    assertFalse(formatted.contains("foo@example.com"));
                    assertFalse(formatted.contains("王小明"));
                });
    }

    @Test
    void longMessageOverLimitKeepsOriginalMessage() {
        contextRunner
                .withPropertyValues(
                        "safe-output.log.max-message-length=20",
                        "safe-output.log.regex-fallback.enabled=true",
                        "safe-output.rules[0].name=logName",
                        "safe-output.rules[0].keys[0]=chineseName",
                        "safe-output.rules[0].type=CHINESE_NAME")
                .run(context -> {
                    PatternLayout layout = safeOutputLayout();

                    String formatted = layout.toSerializable(event("prefix chineseName=张三 contact=foo@example.com"));

                    assertTrue(formatted.contains("chineseName=张三"));
                    assertTrue(formatted.contains("contact=foo@example.com"));
                });
    }

    @Test
    void springMaxValueLengthDrivesRealLog4j2Converter() {
        contextRunner
                .withPropertyValues(
                        "safe-output.log.max-value-length=5",
                        "safe-output.rules[0].name=logName",
                        "safe-output.rules[0].keys[0]=chineseName",
                        "safe-output.rules[0].type=CHINESE_NAME")
                .run(context -> {
                    PatternLayout layout = safeOutputLayout();

                    String formatted = layout.toSerializable(event("chineseName=张三丰张三丰"));

                    assertTrue(formatted.contains("chineseName=张三丰张三丰"));
                });
    }

    @Test
    void springMaxRuleKeysDrivesRealLog4j2Converter() {
        contextRunner
                .withPropertyValues(
                        "safe-output.log.max-rule-keys=1",
                        "safe-output.rules[0].name=logName",
                        "safe-output.rules[0].keys[0]=chineseName",
                        "safe-output.rules[0].type=CHINESE_NAME")
                .run(context -> {
                    PatternLayout layout = safeOutputLayout();

                    String formatted = layout.toSerializable(event("chineseName=张三 mobile=13812345678"));

                    assertTrue(formatted.contains("chineseName=张三"));
                    assertTrue(formatted.contains("mobile=13812345678"));
                });
    }

    @Test
    void springKeyValueRuleDisabledDoesNotDisableFallback() {
        contextRunner
                .withPropertyValues(
                        "safe-output.log.key-value-rule-enabled=false",
                        "safe-output.log.regex-fallback.enabled=true",
                        "safe-output.rules[0].name=logName",
                        "safe-output.rules[0].keys[0]=chineseName",
                        "safe-output.rules[0].type=CHINESE_NAME")
                .run(context -> {
                    PatternLayout layout = safeOutputLayout();

                    String formatted = layout.toSerializable(event("chineseName=张三 contact=foo@example.com"));

                    assertTrue(formatted.contains("chineseName=张三"));
                    assertTrue(formatted.contains("contact=foo****@example.com"));
                    assertFalse(formatted.contains("foo@example.com"));
                });
    }

    @Test
    void springLogDisabledKeepsOriginalMessage() {
        contextRunner
                .withPropertyValues(
                        "safe-output.log.enabled=false",
                        "safe-output.rules[0].name=logName",
                        "safe-output.rules[0].keys[0]=chineseName",
                        "safe-output.rules[0].type=CHINESE_NAME")
                .run(context -> {
                    PatternLayout layout = safeOutputLayout();

                    String formatted = layout.toSerializable(event("chineseName=张三 email=foo@example.com"));

                    assertTrue(formatted.contains("chineseName=张三"));
                    assertTrue(formatted.contains("email=foo@example.com"));
                });
    }

    private static PatternLayout safeOutputLayout() {
        return PatternLayout.newBuilder()
                .withPattern("%safeOutputMsg")
                .withAlwaysWriteExceptions(false)
                .build();
    }

    private static LogEvent event(String message) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName("safe-output-starter-test")
                .setMessage(new SimpleMessage(message))
                .build();
    }

    private static String repeat(String value, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    @Configuration
    static class CustomStrategyConfiguration {

        @Bean
        MaskStrategy mobileMStrategy() {
            return new MaskStrategy() {
                @Override
                public String type() {
                    return "mobileM";
                }

                @Override
                public String mask(String rawValue, MaskContext context) {
                    return "mobileM-" + rawValue.substring(rawValue.length() - 4);
                }
            };
        }
    }
}
