package com.safeoutput.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaskContractTest {

    @Test
    void maskTypeFallsBackToUnknownForUnsupportedCode() {
        assertEquals(MaskType.UNKNOWN, MaskType.fromCode(null));
        assertEquals(MaskType.UNKNOWN, MaskType.fromCode(""));
        assertEquals(MaskType.UNKNOWN, MaskType.fromCode("session-token"));
        assertEquals(MaskType.MOBILE, MaskType.fromCode("mobile"));
        assertEquals(MaskType.MOBILE, MaskType.fromCode("phone"));
        assertEquals(MaskType.ID_CARD, MaskType.fromCode("ID_CARD"));
        assertEquals(MaskType.BANK_CARD, MaskType.fromCode("bank-card"));
        assertEquals(MaskType.CHINESE_NAME, MaskType.fromCode("chinese_name"));
        assertEquals(MaskType.ADDRESS, MaskType.fromCode("address"));
        assertEquals(MaskType.PASSWORD, MaskType.fromCode("password"));
        assertEquals(MaskType.DEFAULT, MaskType.fromCode("default"));
    }

    @Test
    void maskContextCarriesClassificationSceneAndOriginalValueWithoutFrameworkDependencies() {
        MaskContext context = MaskContext.builder()
                .maskType(MaskType.EMAIL)
                .scene(MaskScene.RESPONSE)
                .path("$.user.email")
                .fieldName("email")
                .rawValue("a@example.com")
                .build();

        assertEquals(MaskType.EMAIL, context.getMaskType());
        assertEquals(MaskScene.RESPONSE, context.getScene());
        assertEquals("$.user.email", context.getPath());
        assertEquals("email", context.getFieldName());
        assertEquals("a@example.com", context.getRawValue());
    }

    @Test
    void unchangedResultKeepsNullAndShortValuesSafe() {
        MaskContext nullContext = MaskContext.builder()
                .maskType(MaskType.UNKNOWN)
                .scene(MaskScene.LOG)
                .rawValue(null)
                .build();
        MaskResult nullResult = MaskResult.unchanged(nullContext);

        assertSame(nullContext, nullResult.getContext());
        assertNull(nullResult.getValue());
        assertFalse(nullResult.isMasked());

        MaskContext shortContext = MaskContext.builder()
                .maskType(MaskType.MOBILE)
                .scene(MaskScene.RESPONSE)
                .rawValue("1")
                .build();
        MaskResult shortResult = MaskResult.unchanged(shortContext);

        assertEquals("1", shortResult.getValue());
        assertFalse(shortResult.isMasked());
    }

    @Test
    void maskStrategySpiUsesStableContextAndResultContract() {
        MaskStrategy strategy = new PrefixMaskStrategy();
        MaskContext context = MaskContext.builder()
                .maskType(MaskType.EMAIL)
                .scene(MaskScene.RESPONSE)
                .rawValue("alice@example.com")
                .build();

        MaskResult result = strategy.apply(context);

        assertEquals(MaskType.EMAIL, strategy.supportType());
        assertEquals("***@example.com", result.getValue());
        assertTrue(result.isMasked());
        assertSame(context, result.getContext());
    }

    @Test
    void strategyApplySafelySkipsUnknownNullEmptyAndTooShortValues() {
        MaskStrategy strategy = new PrefixMaskStrategy();

        assertUnchanged(strategy.apply(MaskContext.builder()
                .maskType(MaskType.UNKNOWN)
                .scene(MaskScene.RESPONSE)
                .rawValue("alice@example.com")
                .build()));
        assertUnchanged(strategy.apply(MaskContext.builder()
                .maskType(MaskType.EMAIL)
                .scene(MaskScene.RESPONSE)
                .rawValue(null)
                .build()));
        assertUnchanged(strategy.apply(MaskContext.builder()
                .maskType(MaskType.EMAIL)
                .scene(MaskScene.RESPONSE)
                .rawValue("")
                .build()));
        assertUnchanged(strategy.apply(MaskContext.builder()
                .maskType(MaskType.EMAIL)
                .scene(MaskScene.RESPONSE)
                .rawValue("a")
                .build()));
    }

    @Test
    void strategyApplySkipsTypesOutsideStrategySupport() {
        MaskStrategy strategy = new PrefixMaskStrategy();

        MaskResult result = strategy.apply(MaskContext.builder()
                .maskType(MaskType.MOBILE)
                .scene(MaskScene.RESPONSE)
                .rawValue("13800138000")
                .build());

        assertUnchanged(result);
    }

    private static final class PrefixMaskStrategy implements MaskStrategy {

        @Override
        public MaskType supportType() {
            return MaskType.EMAIL;
        }

        @Override
        public String mask(String rawValue, MaskContext context) {
            return "***@example.com";
        }
    }

    private static void assertUnchanged(MaskResult result) {
        assertEquals(result.getContext().getRawValue(), result.getValue());
        assertFalse(result.isMasked());
    }
}
