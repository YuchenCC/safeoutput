package com.safeoutput.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class SafeOutputMaskServiceTest {

    @Test
    void masksExplicitBuiltInTypes() {
        SafeOutputMaskService service = new DefaultSafeOutputMaskService(MaskStrategyRegistry.withBuiltIns());

        assertEquals("138****5678", service.mask("13812345678", "MOBILE"));
        assertEquals("张*", service.mask("张三", "CHINESE_NAME"));
    }

    @Test
    void masksExplicitCustomTypeWhenStrategyIsRegistered() {
        SafeOutputMaskService service = new DefaultSafeOutputMaskService(MaskStrategyRegistry.withBuiltIns(
                Arrays.asList(new FixedStrategy("mobileM", "custom-masked"))));

        assertEquals("custom-masked", service.mask("13812345678", "mobileM"));
    }

    @Test
    void registeredCustomTypeIsCaseInsensitiveAndDoesNotUseUnknownFallback() {
        final AtomicInteger unknownCount = new AtomicInteger();
        SafeOutputMaskService service = new DefaultSafeOutputMaskService(MaskStrategyRegistry.withBuiltIns(
                Arrays.asList(new FixedStrategy("mobileM", "custom-masked"))),
                null, null, null, new UnknownTypeRecorder() {
                    @Override
                    public void recordUnknownType(String type, MaskScene scene) {
                        unknownCount.incrementAndGet();
                    }
                });

        assertEquals("custom-masked", service.mask("abc123", "MOBILEM"));
        assertEquals(0, unknownCount.get());
    }

    @Test
    void unknownTypeFallsBackToDefaultAndStrategyFailureReturnsOriginalValue() {
        AtomicInteger unknownCount = new AtomicInteger();
        SafeOutputMaskService unknown = new DefaultSafeOutputMaskService(MaskStrategyRegistry.withBuiltIns(),
                null, null, null, new UnknownTypeRecorder() {
                    @Override
                    public void recordUnknownType(String type, MaskScene scene) {
                        if ("mobilem".equals(type) && scene == MaskScene.MANUAL) {
                            unknownCount.incrementAndGet();
                        }
                    }
                });
        SafeOutputMaskService broken = new DefaultSafeOutputMaskService(MaskStrategyRegistry.withBuiltIns(
                Arrays.asList(new BrokenStrategy())));

        assertEquals("****", unknown.mask("13812345678", "mobileM"));
        assertEquals(1, unknownCount.get());
        assertEquals("secret", broken.mask("secret", "BROKEN"));
    }

    @Test
    void masksObjectsWithResponseRulesAndDoesNotScanPlainStringsByDefault() {
        SafeOutputMaskService service = new DefaultSafeOutputMaskService(MaskStrategyRegistry.withBuiltIns(),
                new ObjectMasker(MaskStrategyRegistry.withBuiltIns(), MaskRuleMatcher.withConfiguredRules(
                        Arrays.asList(MaskRule.configured("profileName")
                                .paths(Arrays.asList("$.profile.realName"))
                                .type(MaskTypes.CHINESE_NAME)
                                .build())),
                        ObjectMaskerOptions.defaults()));
        UserPayload payload = new UserPayload();
        payload.mobile = "13812345678";
        payload.remark = "请联系 13912345678 或 foo@example.com";
        payload.profile = new ProfilePayload();
        payload.profile.realName = "王小明";
        payload.profile.email = "foo@example.com";
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("password", "secret");
        attributes.put("remark", "手机号 13712345678");

        UserPayload masked = (UserPayload) service.maskObject(payload);
        Map<?, ?> maskedMap = (Map<?, ?>) service.maskObject(attributes);

        assertSame(payload, masked);
        assertEquals("138****5678", masked.mobile);
        assertEquals("请联系 13912345678 或 foo@example.com", masked.remark);
        assertEquals("王*明", masked.profile.realName);
        assertEquals("foo****@example.com", masked.profile.email);
        assertEquals("********", maskedMap.get("password"));
        assertEquals("手机号 13712345678", maskedMap.get("remark"));
    }

    @Test
    void strongTextMasksDefaultFallbackTypesAndKeyValuesOnlyWhenExplicitlyCalled() {
        SafeOutputMaskService service = new DefaultSafeOutputMaskService(MaskStrategyRegistry.withBuiltIns(),
                new ObjectMasker(MaskStrategyRegistry.withBuiltIns(), MaskRuleMatcher.withDefaultRules(),
                        ObjectMaskerOptions.defaults()));

        String masked = service.maskStrong("mobile=13812345678 contact 13912345678 foo@example.com "
                + "id 11010519491231002X bank 6222021234567890123 name 张三");

        assertEquals("mobile=138****5678 contact 139****5678 foo****@example.com "
                + "id 110105********002X bank 6222021234567890123 name 张三", masked);
    }

    @Test
    void strongObjectMasksStringsInsideMapAndBean() {
        SafeOutputMaskService service = new DefaultSafeOutputMaskService(MaskStrategyRegistry.withBuiltIns(),
                new ObjectMasker(MaskStrategyRegistry.withBuiltIns(), MaskRuleMatcher.withDefaultRules(),
                        ObjectMaskerOptions.defaults()));
        UserPayload payload = new UserPayload();
        payload.remark = "联系 13912345678";
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("message", "email foo@example.com");
        map.put("payload", payload);

        Map<?, ?> masked = (Map<?, ?>) service.maskObjectStrong(map);

        assertEquals("email foo****@example.com", masked.get("message"));
        assertEquals("联系 139****5678", ((UserPayload) masked.get("payload")).remark);
    }

    @Test
    void configuredStrongFallbackTypesCanEnableBankCardScan() {
        SafeOutputMaskService service = new DefaultSafeOutputMaskService(MaskStrategyRegistry.withBuiltIns(),
                new ObjectMasker(MaskStrategyRegistry.withBuiltIns(), MaskRuleMatcher.withDefaultRules(),
                        ObjectMaskerOptions.defaults()),
                Arrays.asList(MaskTypes.BANK_CARD));

        assertEquals("bank 622202*********0123", service.maskStrong("bank 6222021234567890123"));
    }

    @Test
    void manualCallsRecordManualSceneWithoutResponseApiRisk() {
        final AtomicInteger manualCount = new AtomicInteger();
        MaskEventRecorder recorder = new MaskEventRecorder() {
            @Override
            public void recordMask(MaskScene scene, String type, long elapsedNanos) {
                if (scene == MaskScene.MANUAL) {
                    manualCount.incrementAndGet();
                }
            }
        };
        ObjectMasker objectMasker = new ObjectMasker(MaskStrategyRegistry.withBuiltIns(),
                MaskRuleMatcher.withDefaultRules(), null, ObjectMaskerOptions.defaults(), null, recorder);
        SafeOutputMaskService service = new DefaultSafeOutputMaskService(MaskStrategyRegistry.withBuiltIns(),
                objectMasker,
                null,
                recorder);
        UserPayload payload = new UserPayload();
        payload.mobile = "13812345678";

        service.mask("13812345678", MaskTypes.MOBILE);
        service.maskObject(payload);
        service.maskStrong("foo@example.com");

        assertEquals(3, manualCount.get());
    }

    private static final class FixedStrategy implements MaskStrategy {

        private final String type;
        private final String masked;

        private FixedStrategy(String type, String masked) {
            this.type = type;
            this.masked = masked;
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public String mask(String rawValue, MaskContext context) {
            return masked;
        }
    }

    private static final class BrokenStrategy implements MaskStrategy {

        @Override
        public String type() {
            return "BROKEN";
        }

        @Override
        public String mask(String rawValue, MaskContext context) {
            throw new IllegalStateException("boom");
        }
    }

    private static final class UserPayload {

        private String mobile;

        private String remark;

        private ProfilePayload profile;
    }

    private static final class ProfilePayload {

        private String realName;

        private String email;
    }
}
