package com.safeoutput.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

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
    void unknownTypeAndStrategyFailureReturnOriginalValue() {
        SafeOutputMaskService unknown = new DefaultSafeOutputMaskService(MaskStrategyRegistry.withBuiltIns());
        SafeOutputMaskService broken = new DefaultSafeOutputMaskService(MaskStrategyRegistry.withBuiltIns(
                Arrays.asList(new BrokenStrategy())));

        assertEquals("13812345678", unknown.mask("13812345678", "mobileM"));
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
