package com.safeoutput.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class SensitiveFieldResolverTest {

    @Test
    void annotatedFieldResolvesMaskTypeWithoutConfiguredRule() throws Exception {
        SensitiveFieldResolver resolver = new SensitiveFieldResolver(MaskRuleMatcher.withDefaultRules());

        RuleMatch match = resolver.resolve(field("displayName"), "$.displayName").get();

        assertEquals(RuleAction.MASK, match.getAction());
        assertEquals(MaskTypes.CHINESE_NAME, match.getMaskType());
        assertEquals(RuleSource.ANNOTATION, match.getSource());
    }

    @Test
    void annotationTakesPrecedenceOverConfiguredKeyRule() throws Exception {
        MaskRuleMatcher matcher = MaskRuleMatcher.withConfiguredRules(Arrays.asList(
                MaskRule.configured("displayNameAsDefault")
                        .keys(Arrays.asList("displayName"))
                        .type(MaskType.DEFAULT)
                        .build()));
        SensitiveFieldResolver resolver = new SensitiveFieldResolver(matcher);

        RuleMatch match = resolver.resolve(field("displayName"), "$.displayName").get();

        assertEquals(MaskTypes.CHINESE_NAME, match.getMaskType());
        assertEquals(RuleSource.ANNOTATION, match.getSource());
    }

    @Test
    void fieldIgnoreTakesPrecedenceOverAnnotationAndConfiguredRule() throws Exception {
        MaskRuleMatcher matcher = MaskRuleMatcher.builder()
                .ignoreKeys(Arrays.asList("displayName"))
                .configuredRules(Arrays.asList(
                        MaskRule.configured("displayNameAsDefault")
                                .keys(Arrays.asList("displayName"))
                                .type(MaskType.DEFAULT)
                                .build()))
                .build();
        SensitiveFieldResolver resolver = new SensitiveFieldResolver(matcher);

        RuleMatch match = resolver.resolve(field("displayName"), "$.displayName").get();

        assertEquals(RuleAction.IGNORE, match.getAction());
        assertEquals(RuleSource.FIELD_IGNORE, match.getSource());
    }

    @Test
    void fieldMetadataIsCachedAcrossRepeatedResolution() throws Exception {
        SensitiveFieldResolver resolver = new SensitiveFieldResolver(MaskRuleMatcher.withDefaultRules());
        Field field = field("displayName");

        resolver.resolve(field, "$.displayName");
        resolver.resolve(field, "$.customerName");

        assertEquals(1, resolver.cachedFieldCount());
    }

    private static Field field(String name) throws Exception {
        Field field = CustomerPayload.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static final class CustomerPayload {

        @Desensitize(type = MaskTypes.CHINESE_NAME)
        private String displayName;
    }
}
