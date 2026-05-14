package com.safeoutput.spring.boot.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.safeoutput.core.MaskTypes;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputAutoConfiguration;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputProperties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SafeOutputPropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SafeOutputAutoConfiguration.class);

    @Test
    void missingConfigurationBindsSafeDefaults() {
        contextRunner.run(context -> {
            SafeOutputProperties properties = context.getBean(SafeOutputProperties.class);

            assertTrue(properties.isEnabled());
            assertTrue(properties.getResponse().isEnabled());
            assertTrue(properties.getLog().isEnabled());
            assertEquals("*", properties.getMaskChar());
            assertEquals(8, properties.getMaxDepth());
            assertEquals(1000, properties.getMaxCollectionSize());
            assertEquals(5000, properties.getLog().getMaxMessageLength());
            assertEquals(300, properties.getLog().getMaxValueLength());
            assertTrue(properties.getLog().isKeyValueRuleEnabled());
            assertEquals(128, properties.getLog().getMaxRuleKeys());
            assertTrue(properties.getManual().getStrongScan().getTypes().isEmpty());
            assertFalse(properties.getLog().getRegexFallback().isEnabled());
            assertTrue(properties.getLog().getRegexFallback().isIdCardCheckCodeEnabled());
            assertEquals(SafeOutputProperties.UnknownTypePolicy.SKIP,
                    properties.getStrategy().getUnknownTypePolicy());
            assertTrue(properties.getRules().isEmpty());
            assertTrue(properties.getIgnore().getKeys().isEmpty());
        });
    }

    @Test
    void configurationBindsRulesIgnoreReportAndLogLimits() {
        contextRunner
                .withPropertyValues(
                        "safe-output.enabled=false",
                        "safe-output.response.enabled=false",
                        "safe-output.log.enabled=false",
                        "safe-output.mask-char=#",
                        "safe-output.max-depth=4",
                        "safe-output.max-collection-size=25",
                        "safe-output.strategy.unknown-type-policy=SKIP",
                        "safe-output.rules[0].name=customerMobile",
                        "safe-output.rules[0].keys[0]=mobile",
                        "safe-output.rules[0].keys[1]=phone",
                        "safe-output.rules[0].paths[0]=$.customer.mobile",
                        "safe-output.rules[0].type= MOBILE ",
                        "safe-output.rules[0].enabled=false",
                        "safe-output.ignore.keys[0]=productName",
                        "safe-output.ignore.paths[0]=$.items[*].title",
                        "safe-output.ignore.packages[0]=com.example.dto",
                        "safe-output.ignore.apis[0].pattern=/internal/plain-mobile",
                        "safe-output.ignore.apis[0].reason=business plaintext lookup",
                        "safe-output.report.enabled=true",
                        "safe-output.report.include-api-metrics=true",
                        "safe-output.report.include-field-path=true",
                        "safe-output.report.include-raw-value=true",
                        "safe-output.report.directory=/tmp/safe-output",
                        "safe-output.report.file-prefix=pilot-report",
                        "safe-output.report.interval-millis=30000",
                        "safe-output.report.retain-files=7",
                        "safe-output.log.framework=LOG4J2",
                        "safe-output.log.max-message-length=2000",
                        "safe-output.log.max-value-length=120",
                        "safe-output.log.key-value-rule-enabled=false",
                        "safe-output.log.max-rule-keys=12",
                        "safe-output.log.regex-fallback.enabled=true",
                        "safe-output.log.regex-fallback.id-card-check-code-enabled=false",
                        "safe-output.log.regex-fallback.types[0]=MOBILE",
                        "safe-output.log.regex-fallback.types[1]=EMAIL",
                        "safe-output.manual.strong-scan.types[0]=BANK_CARD",
                        "safe-output.rules[1].name=customMobile",
                        "safe-output.rules[1].keys[0]=mobileM",
                        "safe-output.rules[1].type=mobileM")
                .run(context -> {
                    SafeOutputProperties properties = context.getBean(SafeOutputProperties.class);

                    assertFalse(properties.isEnabled());
                    assertFalse(properties.getResponse().isEnabled());
                    assertFalse(properties.getLog().isEnabled());
                    assertEquals("#", properties.getMaskChar());
                    assertEquals(4, properties.getMaxDepth());
                    assertEquals(25, properties.getMaxCollectionSize());
                    assertEquals(SafeOutputProperties.UnknownTypePolicy.SKIP,
                            properties.getStrategy().getUnknownTypePolicy());

                    assertEquals("customerMobile", properties.getRules().get(0).getName());
                    assertEquals(MaskTypes.MOBILE, properties.getRules().get(0).getType());
                    assertFalse(properties.getRules().get(0).isEnabled());
                    assertEquals("phone", properties.getRules().get(0).getKeys().get(1));
                    assertEquals("$.customer.mobile", properties.getRules().get(0).getPaths().get(0));

                    assertEquals("productName", properties.getIgnore().getKeys().get(0));
                    assertEquals("$.items[*].title", properties.getIgnore().getPaths().get(0));
                    assertEquals("com.example.dto", properties.getIgnore().getPackages().get(0));
                    assertEquals("/internal/plain-mobile", properties.getIgnore().getApis().get(0).getPattern());
                    assertEquals("business plaintext lookup", properties.getIgnore().getApis().get(0).getReason());

                    assertTrue(properties.getReport().isEnabled());
                    assertTrue(properties.getReport().isIncludeApiMetrics());
                    assertTrue(properties.getReport().isIncludeFieldPath());
                    assertTrue(properties.getReport().isIncludeRawValue());
                    assertEquals("/tmp/safe-output", properties.getReport().getDirectory());
                    assertEquals("pilot-report", properties.getReport().getFilePrefix());
                    assertEquals(30000, properties.getReport().getIntervalMillis());
                    assertEquals(7, properties.getReport().getRetainFiles());

                    assertEquals("LOG4J2", properties.getLog().getFramework());
                    assertEquals(2000, properties.getLog().getMaxMessageLength());
                    assertEquals(120, properties.getLog().getMaxValueLength());
                    assertFalse(properties.getLog().isKeyValueRuleEnabled());
                    assertEquals(12, properties.getLog().getMaxRuleKeys());
                    assertEquals("BANK_CARD", properties.getManual().getStrongScan().getTypes().get(0));
                    assertTrue(properties.getLog().getRegexFallback().isEnabled());
                    assertFalse(properties.getLog().getRegexFallback().isIdCardCheckCodeEnabled());
                    assertEquals("EMAIL", properties.getLog().getRegexFallback().getTypes().get(1));
                    assertEquals("mobilem", properties.getRules().get(1).getType());
                });
    }
}
