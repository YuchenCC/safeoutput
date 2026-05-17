package com.safeoutput.log4j2;

import com.safeoutput.core.BuiltInMaskStrategies;
import com.safeoutput.core.MaskContext;
import com.safeoutput.core.MainlandIdCards;
import com.safeoutput.core.LogRuleSuggestionCollector;
import com.safeoutput.core.LogRuleSuggestionEvent;
import com.safeoutput.core.MaskRuleMatcher;
import com.safeoutput.core.MaskScene;
import com.safeoutput.core.MaskStrategy;
import com.safeoutput.core.MaskStrategyRegistry;
import com.safeoutput.core.MaskTypes;
import com.safeoutput.core.RuleAction;
import com.safeoutput.core.RuleMatch;
import com.safeoutput.core.UnknownTypeRecorder;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SafeOutputLogMessageMasker {

    private static final Logger LOGGER = Logger.getLogger(SafeOutputLogMessageMasker.class.getName());

    private static final int DEFAULT_MAX_MESSAGE_LENGTH = 5000;

    private static final int DEFAULT_MAX_VALUE_LENGTH = 300;

    private static final int DEFAULT_MAX_RULE_KEYS = 128;

    private static final Pattern KEY_VALUE = Pattern.compile(
            "(\"([A-Za-z][A-Za-z0-9_-]*)\"|'([A-Za-z][A-Za-z0-9_-]*)'|([A-Za-z][A-Za-z0-9_-]*))"
                    + "(\\s*[:=]\\s*)(\"[^\"]*\"|'[^']*'|[^\\s,}]+)");

    private static final Pattern MOBILE_FALLBACK = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");

    private static final Pattern EMAIL_FALLBACK = Pattern.compile(
            "[A-Za-z0-9._%+-]{3,}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private static final Pattern ID_CARD_FALLBACK = Pattern.compile("(?<![0-9A-Za-z])\\d{17}[0-9Xx](?![0-9A-Za-z])");

    private static final Pattern NEARBY_KEY = Pattern.compile("([A-Za-z][A-Za-z0-9_-]*)\\s*[:=]\\s*$");

    private final MaskStrategyRegistry strategyRegistry;

    private final int maxMessageLength;

    private final int maxValueLength;

    private final boolean regexFallbackEnabled;

    private final boolean idCardCheckCodeEnabled;

    private final boolean keyValueRuleEnabled;

    private final Map<String, RuleMatch> keyValueMatches;

    private final LogRuleSuggestionCollector suggestionCollector;

    private final UnknownTypeRecorder unknownTypeRecorder;

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

    SafeOutputLogMessageMasker(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry,
            UnknownTypeRecorder unknownTypeRecorder) {
        this(ruleMatcher, strategyRegistry, DEFAULT_MAX_MESSAGE_LENGTH, DEFAULT_MAX_VALUE_LENGTH, true, true,
                true, DEFAULT_MAX_RULE_KEYS, null, unknownTypeRecorder);
    }

    SafeOutputLogMessageMasker(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry,
            LogRuleSuggestionCollector suggestionCollector) {
        this(ruleMatcher, strategyRegistry, DEFAULT_MAX_MESSAGE_LENGTH, DEFAULT_MAX_VALUE_LENGTH, true, true,
                true, DEFAULT_MAX_RULE_KEYS, suggestionCollector);
    }

    SafeOutputLogMessageMasker(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry,
            int maxMessageLength, int maxValueLength, boolean regexFallbackEnabled, boolean keyValueRuleEnabled,
            int maxRuleKeys) {
        this(ruleMatcher, strategyRegistry, maxMessageLength, maxValueLength, regexFallbackEnabled, true,
                keyValueRuleEnabled, maxRuleKeys);
    }

    SafeOutputLogMessageMasker(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry,
            int maxMessageLength, int maxValueLength, boolean regexFallbackEnabled, boolean idCardCheckCodeEnabled,
            boolean keyValueRuleEnabled, int maxRuleKeys) {
        this(ruleMatcher, strategyRegistry, maxMessageLength, maxValueLength, regexFallbackEnabled,
                idCardCheckCodeEnabled, keyValueRuleEnabled, maxRuleKeys,
                (LogRuleSuggestionCollector) null);
    }

    SafeOutputLogMessageMasker(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry,
            int maxMessageLength, int maxValueLength, boolean regexFallbackEnabled, boolean idCardCheckCodeEnabled,
            boolean keyValueRuleEnabled, int maxRuleKeys, UnknownTypeRecorder unknownTypeRecorder) {
        this(ruleMatcher, strategyRegistry, maxMessageLength, maxValueLength, regexFallbackEnabled,
                idCardCheckCodeEnabled, keyValueRuleEnabled, maxRuleKeys, null, unknownTypeRecorder);
    }

    SafeOutputLogMessageMasker(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry,
            int maxMessageLength, int maxValueLength, boolean regexFallbackEnabled, boolean idCardCheckCodeEnabled,
            boolean keyValueRuleEnabled, int maxRuleKeys, LogRuleSuggestionCollector suggestionCollector) {
        this(ruleMatcher, strategyRegistry, maxMessageLength, maxValueLength, regexFallbackEnabled,
                idCardCheckCodeEnabled, keyValueRuleEnabled, maxRuleKeys, suggestionCollector, null);
    }

    SafeOutputLogMessageMasker(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry,
            int maxMessageLength, int maxValueLength, boolean regexFallbackEnabled, boolean idCardCheckCodeEnabled,
            boolean keyValueRuleEnabled, int maxRuleKeys, LogRuleSuggestionCollector suggestionCollector,
            UnknownTypeRecorder unknownTypeRecorder) {
        this(strategyRegistry, maxMessageLength, maxValueLength, regexFallbackEnabled, idCardCheckCodeEnabled,
                keyValueRuleEnabled, ruleMatcher.logKeyMatches(Math.max(1, maxRuleKeys)), suggestionCollector,
                unknownTypeRecorder);
    }

    private SafeOutputLogMessageMasker(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry,
            int maxMessageLength, int maxValueLength, boolean regexFallbackEnabled, boolean idCardCheckCodeEnabled) {
        this(ruleMatcher, strategyRegistry, maxMessageLength, maxValueLength, regexFallbackEnabled,
                idCardCheckCodeEnabled, true, DEFAULT_MAX_RULE_KEYS);
    }

    private SafeOutputLogMessageMasker(MaskStrategyRegistry strategyRegistry, int maxMessageLength, int maxValueLength,
            boolean regexFallbackEnabled, boolean idCardCheckCodeEnabled, boolean keyValueRuleEnabled,
            Map<String, RuleMatch> keyValueMatches, LogRuleSuggestionCollector suggestionCollector,
            UnknownTypeRecorder unknownTypeRecorder) {
        this.strategyRegistry = strategyRegistry;
        this.maxMessageLength = Math.max(1, maxMessageLength);
        this.maxValueLength = Math.max(1, maxValueLength);
        this.regexFallbackEnabled = regexFallbackEnabled;
        this.idCardCheckCodeEnabled = idCardCheckCodeEnabled;
        this.keyValueRuleEnabled = keyValueRuleEnabled;
        this.keyValueMatches = keyValueRuleEnabled ? keyValueMatches
                : java.util.Collections.<String, RuleMatch>emptyMap();
        this.suggestionCollector = suggestionCollector;
        this.unknownTypeRecorder = unknownTypeRecorder;
    }

    String mask(String message) {
        try {
            return maskSafely(message);
        } catch (RuntimeException ex) {
            return message;
        }
    }

    private String maskSafely(String message) {
        if (message == null || message.isEmpty() || message.length() > maxMessageLength) {
            return message;
        }
        // 第一阶段只处理带字段名上下文的片段，例如 mobile=... 或 "email":"..."。
        if (!keyValueRuleEnabled || keyValueMatches.isEmpty()) {
            return regexFallbackEnabled ? maskFallback(message) : message;
        }
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
        RuleMatch match = keyValueMatches.get(normalizeKey(key));
        if (match == null || match.getAction() != RuleAction.MASK) {
            return value;
        }
        String rawValue = unquote(value);
        if (rawValue.length() > maxValueLength) {
            return value;
        }
        Optional<MaskStrategy> strategy = strategyRegistry.find(match.getMaskType());
        String effectiveType = match.getMaskType();
        if (!strategy.isPresent()) {
            // 日志输出链路必须 fail-open；未知 type 先告警，再使用 DEFAULT 策略兜底当前值。
            LOGGER.warning("Fallback log masking to default because no strategy registered for type "
                    + MaskTypes.normalize(match.getMaskType()));
            recordUnknownType(match.getMaskType());
            strategy = defaultStrategy();
            effectiveType = MaskTypes.DEFAULT;
        }
        // key-value 命中时只脱敏 value，保留原始 key 和引号形态，降低日志格式兼容风险。
        String masked = strategy.get().mask(rawValue, MaskContext.builder()
                .maskType(effectiveType)
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
                recordSuggestion(message, matcher.start(), type);
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

    private static String normalizeKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        return key.trim().toLowerCase(Locale.ENGLISH);
    }

    private Optional<MaskStrategy> defaultStrategy() {
        Optional<MaskStrategy> strategy = strategyRegistry.find(MaskTypes.DEFAULT);
        if (strategy.isPresent()) {
            return strategy;
        }
        return Optional.of(BuiltInMaskStrategies.get(MaskTypes.DEFAULT));
    }

    private void recordUnknownType(String type) {
        if (unknownTypeRecorder != null) {
            unknownTypeRecorder.recordUnknownType(MaskTypes.normalize(type), MaskScene.LOG);
        }
    }

    private void recordSuggestion(String message, int valueStart, String type) {
        if (suggestionCollector == null) {
            return;
        }
        try {
            String nearbyKey = nearbyKey(message, valueStart);
            if (nearbyKey == null || keyValueMatches.containsKey(normalizeKey(nearbyKey))) {
                return;
            }
            String normalizedKey = normalizeKey(nearbyKey);
            String normalizedType = MaskTypes.normalize(type);
            // evidence 只保留 key 与 type 形态，不保存命中值或完整日志。
            suggestionCollector.record(new LogRuleSuggestionEvent(normalizedKey, normalizedType,
                    normalizedKey + "=<" + normalizedType + ">", System.currentTimeMillis()));
        } catch (RuntimeException ex) {
            // 日志线索采集失败不能影响日志输出。
        }
    }

    private static String nearbyKey(String message, int valueStart) {
        int from = Math.max(0, valueStart - 80);
        Matcher matcher = NEARBY_KEY.matcher(message.substring(from, valueStart));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
