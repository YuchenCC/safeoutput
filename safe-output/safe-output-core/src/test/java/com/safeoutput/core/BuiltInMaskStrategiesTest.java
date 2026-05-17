package com.safeoutput.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class BuiltInMaskStrategiesTest {

    @Test
    void exposesEveryMvpBuiltInStrategyExceptUnknown() {
        Set<MaskType> expectedTypes = EnumSet.of(
                MaskType.MOBILE,
                MaskType.ID_CARD,
                MaskType.BANK_CARD,
                MaskType.EMAIL,
                MaskType.CHINESE_NAME,
                MaskType.ADDRESS,
                MaskType.PASSWORD,
                MaskType.DEFAULT);

        for (MaskType type : expectedTypes) {
            assertNotNull(BuiltInMaskStrategies.get(type));
            assertEquals(type, BuiltInMaskStrategies.get(type).supportType());
        }
        assertFalse(BuiltInMaskStrategies.supports(MaskType.UNKNOWN));
    }

    @Test
    void masksMobileOnlyWhenValueLooksLikeMainlandMobileNumber() {
        assertMasked(MaskType.MOBILE, "13812345678", "138****5678");
        assertUnchanged(MaskType.MOBILE, null);
        assertUnchanged(MaskType.MOBILE, "");
        assertUnchanged(MaskType.MOBILE, "138");
        assertUnchanged(MaskType.MOBILE, "23812345678");
        assertUnchanged(MaskType.MOBILE, "1381234567a");
    }

    @Test
    void masksIdCardOnlyAfterStrictMainlandIdCardValidation() {
        assertMasked(MaskType.ID_CARD, "11010519491231002X", "110105********002X");
        assertMasked(MaskType.ID_CARD, "11010519491231002x", "110105********002x");
        assertUnchanged(MaskType.ID_CARD, null);
        assertUnchanged(MaskType.ID_CARD, "");
        assertUnchanged(MaskType.ID_CARD, "110105");
        assertUnchanged(MaskType.ID_CARD, "110105199902300029");
        assertUnchanged(MaskType.ID_CARD, "110105194912310021");
        assertUnchanged(MaskType.ID_CARD, "123456789012345678");
        assertUnchanged(MaskType.ID_CARD, "110105189912310029");
    }

    @Test
    void masksIdCardDirectlyWhenContextExplicitlyIdentifiesTheType() {
        MaskResult result = BuiltInMaskStrategies.get(MaskType.ID_CARD).apply(MaskContext.builder()
                .maskType(MaskType.ID_CARD)
                .scene(MaskScene.RESPONSE)
                .fieldName("idCard")
                .rawValue("110105199902300029")
                .build());

        assertTrue(result.isMasked());
        assertEquals("110105********0029", result.getValue());
    }

    @Test
    void masksBankCardOnlyForConfirmedBankCardType() {
        assertMasked(MaskType.BANK_CARD, "6222021234567890123", "622202*********0123");
        assertUnchanged(MaskType.BANK_CARD, null);
        assertUnchanged(MaskType.BANK_CARD, "");
        assertUnchanged(MaskType.BANK_CARD, "6222");
        assertUnchanged(MaskType.BANK_CARD, "622202123456789012a");

        MaskStrategy mobileStrategy = BuiltInMaskStrategies.get(MaskType.MOBILE);
        MaskResult result = mobileStrategy.apply(MaskContext.builder()
                .maskType(MaskType.BANK_CARD)
                .rawValue("6222021234567890123")
                .build());
        assertFalse(result.isMasked());
        assertEquals("6222021234567890123", result.getValue());
    }

    @Test
    void masksEmailWhenItHasLocalPartAndDomain() {
        assertMasked(MaskType.EMAIL, "zhangsan@example.com", "zha****@example.com");
        assertUnchanged(MaskType.EMAIL, null);
        assertUnchanged(MaskType.EMAIL, "");
        assertUnchanged(MaskType.EMAIL, "z@e.com");
        assertUnchanged(MaskType.EMAIL, "zhangsan.example.com");
    }

    @Test
    void masksGeneralNamesByKeepingReadableEdges() {
        assertMasked(MaskType.CHINESE_NAME, "张三", "张*");
        assertMasked(MaskType.CHINESE_NAME, "王小明", "王*明");
        assertMasked(MaskType.CHINESE_NAME, "迪丽热巴", "迪**巴");
        assertMasked(MaskType.CHINESE_NAME, "Alice", "A***e");
        assertMasked(MaskType.CHINESE_NAME, "Michael Zhang", "M***********g");
        assertMasked(MaskType.CHINESE_NAME, "张 Michael", "张*******l");
        assertMasked(MaskType.CHINESE_NAME, "A", "*");
        assertUnchanged(MaskType.CHINESE_NAME, null);
        assertUnchanged(MaskType.CHINESE_NAME, "");
    }

    @Test
    void masksAddressByKeepingTheCoarseLocation() {
        assertMasked(MaskType.ADDRESS, "福建省福州市鼓楼区湖东路1号", "福建省福州市****");
        assertUnchanged(MaskType.ADDRESS, null);
        assertUnchanged(MaskType.ADDRESS, "");
        assertUnchanged(MaskType.ADDRESS, "福州");
        assertUnchanged(MaskType.ADDRESS, "No.1 Road");
    }

    @Test
    void masksPasswordWithFixedAsterisks() {
        assertMasked(MaskType.PASSWORD, "abc123456", "********");
        assertUnchanged(MaskType.PASSWORD, null);
        assertUnchanged(MaskType.PASSWORD, "");
        assertUnchanged(MaskType.PASSWORD, "a");
    }

    @Test
    void masksDefaultByReplacingWholeContent() {
        assertMasked(MaskType.DEFAULT, "abcdef", "****");
        assertUnchanged(MaskType.DEFAULT, null);
        assertUnchanged(MaskType.DEFAULT, "");
        assertMasked(MaskType.DEFAULT, "abc", "****");
    }

    private static void assertMasked(MaskType type, String rawValue, String expectedValue) {
        MaskResult result = apply(type, rawValue);

        assertTrue(result.isMasked());
        assertEquals(expectedValue, result.getValue());
    }

    private static void assertUnchanged(MaskType type, String rawValue) {
        MaskResult result = apply(type, rawValue);

        assertFalse(result.isMasked());
        assertEquals(rawValue, result.getValue());
    }

    private static MaskResult apply(MaskType type, String rawValue) {
        return BuiltInMaskStrategies.get(type).apply(MaskContext.builder()
                .maskType(type)
                .rawValue(rawValue)
                .build());
    }
}
