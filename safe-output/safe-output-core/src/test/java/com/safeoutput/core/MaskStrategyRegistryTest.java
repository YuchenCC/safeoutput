package com.safeoutput.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class MaskStrategyRegistryTest {

    @Test
    void findsBuiltInStrategiesByMaskType() {
        MaskStrategyRegistry registry = MaskStrategyRegistry.withBuiltIns();

        Optional<MaskStrategy> strategy = registry.find(MaskType.MOBILE);

        assertTrue(strategy.isPresent());
        assertSame(BuiltInMaskStrategies.get(MaskType.MOBILE), strategy.get());
        assertFalse(registry.find(MaskType.UNKNOWN).isPresent());
        assertFalse(registry.find((String) null).isPresent());
    }

    @Test
    void registersAdditionalStrategies() {
        MaskStrategy custom = new FixedMaskStrategy(MaskType.PASSWORD, "custom-password");

        MaskStrategyRegistry registry = MaskStrategyRegistry.withBuiltIns(Arrays.asList(custom));

        assertSame(custom, registry.find(MaskType.PASSWORD).get());
    }

    @Test
    void normalizesStringTypeLookupForCustomStrategies() {
        MaskStrategy custom = new FixedMaskStrategy(" mobileM ", "custom-mobile");

        MaskStrategyRegistry registry = new MaskStrategyRegistry(Arrays.asList(custom));

        assertSame(custom, registry.find("mobileM").get());
        assertSame(custom, registry.find(" MOBILEM ").get());
    }

    @Test
    void laterStrategyOverridesEarlierStrategyForSameMaskType() {
        MaskStrategy first = new FixedMaskStrategy(MaskType.EMAIL, "first");
        MaskStrategy second = new FixedMaskStrategy(MaskType.EMAIL, "second");

        MaskStrategyRegistry registry = new MaskStrategyRegistry(Arrays.asList(first, second));

        MaskResult result = registry.find(MaskType.EMAIL).get().apply(MaskContext.builder()
                .maskType(MaskType.EMAIL)
                .rawValue("zhangsan@example.com")
                .build());

        assertEquals("second", result.getValue());
    }

    @Test
    void logsWhenStrategyOverridesExistingMaskType() {
        RecordingHandler handler = new RecordingHandler();
        Logger logger = Logger.getLogger(MaskStrategyRegistry.class.getName());
        Level previousLevel = logger.getLevel();
        boolean previousUseParentHandlers = logger.getUseParentHandlers();
        logger.addHandler(handler);
        logger.setLevel(Level.INFO);
        logger.setUseParentHandlers(false);
        try {
            MaskStrategy custom = new FixedMaskStrategy(MaskType.MOBILE, "custom-mobile");

            MaskStrategyRegistry.withBuiltIns(Arrays.asList(custom));

            assertTrue(handler.messageContains("Override mask strategy for type mobile"));
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
            logger.setUseParentHandlers(previousUseParentHandlers);
        }
    }

    private static final class FixedMaskStrategy implements MaskStrategy {

        private final String type;
        private final String maskedValue;

        private FixedMaskStrategy(MaskType type, String maskedValue) {
            this.type = MaskTypes.from(type);
            this.maskedValue = maskedValue;
        }

        private FixedMaskStrategy(String type, String maskedValue) {
            this.type = type;
            this.maskedValue = maskedValue;
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public String mask(String rawValue, MaskContext context) {
            return maskedValue;
        }
    }

    private static final class RecordingHandler extends Handler {

        private String message;

        @Override
        public void publish(LogRecord record) {
            message = record.getMessage();
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        private boolean messageContains(String expected) {
            return message != null && message.contains(expected);
        }
    }
}
