package com.safeoutput.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class BuiltInMaskStrategies {

    private static final Map<String, MaskStrategy> STRATEGIES;

    static {
        Map<String, MaskStrategy> strategies = new LinkedHashMap<String, MaskStrategy>();
        register(strategies, new SimpleMaskStrategy(MaskTypes.MOBILE, (rawValue, context) -> maskMobile(rawValue)));
        register(strategies, new SimpleMaskStrategy(MaskTypes.ID_CARD, BuiltInMaskStrategies::maskIdCard));
        register(strategies, new SimpleMaskStrategy(MaskTypes.BANK_CARD, (rawValue, context) -> maskBankCard(rawValue)));
        register(strategies, new SimpleMaskStrategy(MaskTypes.EMAIL, (rawValue, context) -> maskEmail(rawValue)));
        register(strategies, new SimpleMaskStrategy(MaskTypes.CHINESE_NAME, (rawValue, context) -> maskChineseName(rawValue)));
        register(strategies, new SimpleMaskStrategy(MaskTypes.ADDRESS, (rawValue, context) -> maskAddress(rawValue)));
        register(strategies, new SimpleMaskStrategy(MaskTypes.PASSWORD, (rawValue, context) -> maskPassword(rawValue)));
        register(strategies, new SimpleMaskStrategy(MaskTypes.DEFAULT, (rawValue, context) -> maskDefault(rawValue)));
        STRATEGIES = Collections.unmodifiableMap(strategies);
    }

    private static final Pattern MOBILE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d+");
    private BuiltInMaskStrategies() {
    }

    public static boolean supports(MaskType type) {
        return supports(MaskTypes.from(type));
    }

    public static boolean supports(String type) {
        return STRATEGIES.containsKey(MaskTypes.normalize(type));
    }

    public static MaskStrategy get(MaskType type) {
        return get(MaskTypes.from(type));
    }

    public static MaskStrategy get(String type) {
        return STRATEGIES.get(MaskTypes.normalize(type));
    }

    public static Collection<MaskStrategy> strategies() {
        return STRATEGIES.values();
    }

    private static void register(Map<String, MaskStrategy> strategies, MaskStrategy strategy) {
        strategies.put(MaskTypes.normalize(strategy.type()), strategy);
    }

    private static String maskMobile(String rawValue) {
        if (!MOBILE_PATTERN.matcher(rawValue).matches()) {
            return rawValue;
        }
        return rawValue.substring(0, 3) + "****" + rawValue.substring(7);
    }

    private static String maskIdCard(String rawValue, MaskContext context) {
        // 无上下文 fallback 先做轻量身份证校验；明确字段上下文优先避免敏感值明文输出。
        if (!hasExplicitIdCardContext(context) && !MainlandIdCards.isValid(rawValue)) {
            return rawValue;
        }
        if (rawValue.length() != 18) {
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
        // 姓名类型由规则、注解或调用方显式确认，这里只负责首尾保留的通用姓名脱敏。
        if (rawValue.length() == 1) {
            return "*";
        }
        if (rawValue.length() == 2) {
            return rawValue.substring(0, 1) + "*";
        }
        return rawValue.substring(0, 1) + repeat('*', rawValue.length() - 2)
                + rawValue.substring(rawValue.length() - 1);
    }

    private static String maskAddress(String rawValue) {
        if (rawValue.length() <= 6 || !startsWithChinese(rawValue)) {
            return rawValue;
        }
        return rawValue.substring(0, 6) + "****";
    }

    private static String maskPassword(String rawValue) {
        if (rawValue.length() <= 1) {
            return rawValue;
        }
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

    private static boolean hasExplicitIdCardContext(MaskContext context) {
        return context != null
                && (context.getScene() == MaskScene.RESPONSE
                || hasText(context.getFieldName())
                || hasText(context.getPath()));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private interface MaskFunction {
        String mask(String rawValue, MaskContext context);
    }

    private static final class SimpleMaskStrategy implements MaskStrategy {

        private final String type;
        private final MaskFunction function;

        private SimpleMaskStrategy(String type, MaskFunction function) {
            this.type = MaskTypes.normalize(type);
            this.function = function;
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public String mask(String rawValue, MaskContext context) {
            return function.mask(rawValue, context);
        }
    }
}
