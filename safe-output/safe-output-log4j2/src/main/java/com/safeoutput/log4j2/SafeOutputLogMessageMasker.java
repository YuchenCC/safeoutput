package com.safeoutput.log4j2;

import com.safeoutput.core.MaskContext;
import com.safeoutput.core.MainlandIdCards;
import com.safeoutput.core.MaskRuleMatcher;
import com.safeoutput.core.MaskScene;
import com.safeoutput.core.MaskStrategy;
import com.safeoutput.core.MaskStrategyRegistry;
import com.safeoutput.core.MaskTypes;
import com.safeoutput.core.RuleAction;
import com.safeoutput.core.RuleMatch;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SafeOutputLogMessageMasker {

    private static final int DEFAULT_MAX_MESSAGE_LENGTH = 5000;

    private static final int DEFAULT_MAX_VALUE_LENGTH = 300;

    private static final Pattern KEY_VALUE = Pattern.compile(
            "(\"([A-Za-z][A-Za-z0-9_-]*)\"|'([A-Za-z][A-Za-z0-9_-]*)'|([A-Za-z][A-Za-z0-9_-]*))"
                    + "(\\s*[:=]\\s*)(\"[^\"]*\"|'[^']*'|[^\\s,}]+)");

    private static final Pattern MOBILE_FALLBACK = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");

    private static final Pattern EMAIL_FALLBACK = Pattern.compile(
            "[A-Za-z0-9._%+-]{3,}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private static final Pattern ID_CARD_FALLBACK = Pattern.compile("(?<![0-9A-Za-z])\\d{17}[0-9Xx](?![0-9A-Za-z])");

    private final MaskRuleMatcher ruleMatcher;

    private final MaskStrategyRegistry strategyRegistry;

    private final int maxMessageLength;

    private final int maxValueLength;

    private final boolean regexFallbackEnabled;

    private final boolean idCardCheckCodeEnabled;

    SafeOutputLogMessageMasker() {
        this(DEFAULT_MAX_MESSAGE_LENGTH, DEFAULT_MAX_VALUE_LENGTH, true);
    }

    SafeOutputLogMessageMasker(int maxMessageLength, int maxValueLength, boolean regexFallbackEnabled) {
        this(maxMessageLength, maxValueLength, regexFallbackEnabled, true);
    }

    SafeOutputLogMessageMasker(int maxMessageLength, int maxValueLength, boolean regexFallbackEnabled,
            boolean idCardCheckCodeEnabled) {
        this(MaskRuleMatcher.withDefaultRules(), MaskStrategyRegistry.withBuiltIns(), maxMessageLength,
                maxValueLength, regexFallbackEnabled, idCardCheckCodeEnabled);
    }

    SafeOutputLogMessageMasker(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry) {
        this(ruleMatcher, strategyRegistry, DEFAULT_MAX_MESSAGE_LENGTH, DEFAULT_MAX_VALUE_LENGTH, true, true);
    }

    private SafeOutputLogMessageMasker(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry,
            int maxMessageLength, int maxValueLength, boolean regexFallbackEnabled, boolean idCardCheckCodeEnabled) {
        this.ruleMatcher = ruleMatcher;
        this.strategyRegistry = strategyRegistry;
        this.maxMessageLength = Math.max(1, maxMessageLength);
        this.maxValueLength = Math.max(1, maxValueLength);
        this.regexFallbackEnabled = regexFallbackEnabled;
        this.idCardCheckCodeEnabled = idCardCheckCodeEnabled;
    }

    String mask(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        if (message.length() > maxMessageLength) {
            return message;
        }
        // 第一阶段只处理带字段名上下文的片段，例如 mobile=... 或 "email":"..."。
        Matcher matcher = KEY_VALUE.matcher(message);
        StringBuffer masked = new StringBuffer();
        while (matcher.find()) {
            String key = firstPresent(matcher.group(2), matcher.group(3), matcher.group(4));
            String keyToken = matcher.group(1);
            String separator = matcher.group(5);
            String value = matcher.group(6);
            String replacement = keyToken + separator + maskValue(key, value);
            matcher.appendReplacement(masked, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(masked);
        return regexFallbackEnabled ? maskFallback(masked.toString()) : masked.toString();
    }

    private String maskValue(String key, String value) {
        Optional<RuleMatch> match = ruleMatcher.match(key, null);
        if (!match.isPresent() || match.get().getAction() != RuleAction.MASK) {
            return value;
        }
        String rawValue = unquote(value);
        if (rawValue.length() > maxValueLength) {
            return value;
        }
        Optional<MaskStrategy> strategy = strategyRegistry.find(match.get().getMaskType());
        if (!strategy.isPresent()) {
            return value;
        }
        String masked = strategy.get().mask(rawValue, MaskContext.builder()
                .maskType(match.get().getMaskType())
                .scene(MaskScene.LOG)
                .fieldName(key)
                .rawValue(rawValue)
                .build());
        return requote(value, masked);
    }

    private String maskFallback(String message) {
        // 第二阶段才做 regex fallback，且只覆盖低误伤类型；银行卡不做无上下文兜底。
        String masked = maskFallbackType(message, MOBILE_FALLBACK, MaskTypes.MOBILE);
        masked = maskFallbackType(masked, EMAIL_FALLBACK, MaskTypes.EMAIL);
        return maskFallbackType(masked, ID_CARD_FALLBACK, MaskTypes.ID_CARD);
    }

    private String maskFallbackType(String message, Pattern pattern, String type) {
        Optional<MaskStrategy> strategy = strategyRegistry.find(type);
        if (!strategy.isPresent()) {
            return message;
        }
        Matcher matcher = pattern.matcher(message);
        StringBuffer masked = new StringBuffer();
        while (matcher.find()) {
            String rawValue = matcher.group();
            if (rawValue.length() > maxValueLength) {
                continue;
            }
            // 身份证无上下文兜底先做轻量格式、生日和可选校验位检查，避免误伤普通 18 位编号。
            if (MaskTypes.ID_CARD.equals(type) && !MainlandIdCards.isLikely(rawValue, idCardCheckCodeEnabled)) {
                continue;
            }
            MaskContext.Builder contextBuilder = MaskContext.builder()
                    .maskType(type)
                    .scene(MaskScene.LOG)
                    .rawValue(rawValue);
            if (MaskTypes.ID_CARD.equals(type)) {
                contextBuilder.path("regex-fallback");
            }
            String replacement = strategy.get().mask(rawValue, contextBuilder.build());
            if (!rawValue.equals(replacement)) {
                matcher.appendReplacement(masked, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(masked);
        return masked.toString();
    }

    private static String unquote(String value) {
        if (isQuoted(value)) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String requote(String original, String masked) {
        if (isQuoted(original)) {
            return original.substring(0, 1) + masked + original.substring(original.length() - 1);
        }
        return masked;
    }

    private static boolean isQuoted(String value) {
        return value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")));
    }

    private static String firstPresent(String first, String second, String third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }
}
