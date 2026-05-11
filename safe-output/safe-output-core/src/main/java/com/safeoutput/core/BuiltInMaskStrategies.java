package com.safeoutput.core;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class BuiltInMaskStrategies {

    private static final Map<MaskType, MaskStrategy> STRATEGIES;

    static {
        Map<MaskType, MaskStrategy> strategies = new EnumMap<>(MaskType.class);
        register(strategies, new SimpleMaskStrategy(MaskType.MOBILE, BuiltInMaskStrategies::maskMobile));
        register(strategies, new SimpleMaskStrategy(MaskType.ID_CARD, BuiltInMaskStrategies::maskIdCard));
        register(strategies, new SimpleMaskStrategy(MaskType.BANK_CARD, BuiltInMaskStrategies::maskBankCard));
        register(strategies, new SimpleMaskStrategy(MaskType.EMAIL, BuiltInMaskStrategies::maskEmail));
        register(strategies, new SimpleMaskStrategy(MaskType.CHINESE_NAME, BuiltInMaskStrategies::maskChineseName));
        register(strategies, new SimpleMaskStrategy(MaskType.ADDRESS, BuiltInMaskStrategies::maskAddress));
        register(strategies, new SimpleMaskStrategy(MaskType.PASSWORD, BuiltInMaskStrategies::maskPassword));
        register(strategies, new SimpleMaskStrategy(MaskType.DEFAULT, BuiltInMaskStrategies::maskDefault));
        STRATEGIES = Collections.unmodifiableMap(strategies);
    }

    private static final Pattern MOBILE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d+");
    private static final Pattern CHINESE_NAME_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}");

    private BuiltInMaskStrategies() {
    }

    public static boolean supports(MaskType type) {
        return STRATEGIES.containsKey(type);
    }

    public static MaskStrategy get(MaskType type) {
        return STRATEGIES.get(type);
    }

    public static Collection<MaskStrategy> strategies() {
        return STRATEGIES.values();
    }

    private static void register(Map<MaskType, MaskStrategy> strategies, MaskStrategy strategy) {
        strategies.put(strategy.supportType(), strategy);
    }

    private static String maskMobile(String rawValue) {
        if (!MOBILE_PATTERN.matcher(rawValue).matches()) {
            return rawValue;
        }
        return rawValue.substring(0, 3) + "****" + rawValue.substring(7);
    }

    private static String maskIdCard(String rawValue) {
        if (!MainlandIdCards.isValid(rawValue)) {
            return rawValue;
        }
        return rawValue.substring(0, 6) + "********" + rawValue.substring(14);
    }

    private static String maskBankCard(String rawValue) {
        if (rawValue.length() < 12 || rawValue.length() > 19
                || !DIGITS_PATTERN.matcher(rawValue).matches()) {
            return rawValue;
        }
        return rawValue.substring(0, 6)
                + repeat('*', rawValue.length() - 10)
                + rawValue.substring(rawValue.length() - 4);
    }

    private static String maskEmail(String rawValue) {
        int atIndex = rawValue.indexOf('@');
        if (atIndex < 3 || atIndex != rawValue.lastIndexOf('@') || atIndex == rawValue.length() - 1) {
            return rawValue;
        }
        String domain = rawValue.substring(atIndex + 1);
        if (domain.indexOf('.') <= 0 || domain.endsWith(".")) {
            return rawValue;
        }
        return rawValue.substring(0, 3) + "****" + rawValue.substring(atIndex);
    }

    private static String maskChineseName(String rawValue) {
        if (!CHINESE_NAME_PATTERN.matcher(rawValue).matches()) {
            return rawValue;
        }
        return rawValue.substring(0, 1) + repeat('*', rawValue.length() - 1);
    }

    private static String maskAddress(String rawValue) {
        if (rawValue.length() <= 6 || !startsWithChinese(rawValue)) {
            return rawValue;
        }
        return rawValue.substring(0, 6) + "****";
    }

    private static String maskPassword(String rawValue) {
        return "********";
    }

    private static String maskDefault(String rawValue) {
        if (rawValue.length() <= 4) {
            return rawValue;
        }
        return rawValue.substring(0, 2) + "****" + rawValue.substring(rawValue.length() - 2);
    }

    private static boolean startsWithChinese(String value) {
        char first = value.charAt(0);
        return first >= '\u4e00' && first <= '\u9fa5';
    }

    private static String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private interface MaskFunction {
        String mask(String rawValue);
    }

    private static final class SimpleMaskStrategy implements MaskStrategy {

        private final MaskType type;
        private final MaskFunction function;

        private SimpleMaskStrategy(MaskType type, MaskFunction function) {
            this.type = type;
            this.function = function;
        }

        @Override
        public MaskType supportType() {
            return type;
        }

        @Override
        public String mask(String rawValue, MaskContext context) {
            return function.mask(rawValue);
        }
    }
}
