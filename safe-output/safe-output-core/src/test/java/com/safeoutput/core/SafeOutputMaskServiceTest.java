package com.safeoutput.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

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
}
