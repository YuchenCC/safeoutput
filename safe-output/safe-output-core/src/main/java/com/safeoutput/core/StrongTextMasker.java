package com.safeoutput.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StrongTextMasker {

    private static final int MAX_VALUE_LENGTH = 300;

    private static final Pattern KEY_VALUE = Pattern.compile(
            "(\"([A-Za-z][A-Za-z0-9_-]*)\"|'([A-Za-z][A-Za-z0-9_-]*)'|([A-Za-z][A-Za-z0-9_-]*))"
                    + "(\\s*[:=]\\s*)(\"[^\"]*\"|'[^']*'|[^\\s,}]+)");

    private static final Pattern MOBILE_FALLBACK = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");

    private static final Pattern EMAIL_FALLBACK = Pattern.compile(
            "[A-Za-z0-9._%+-]{3,}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private static final Pattern ID_CARD_FALLBACK = Pattern.compile(
            "(?<![0-9A-Za-z])\\d{17}[0-9Xx](?![0-9A-Za-z])");

    private static final Pattern BANK_CARD_FALLBACK = Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)");

    private final MaskStrategyRegistry strategyRegistry;

    private final Map<String, RuleMatch> keyMatches;

    private final List<String> fallbackTypes;

    StrongTextMasker(MaskStrategyRegistry strategyRegistry, MaskRuleMatcher ruleMatcher) {
        this(strategyRegistry, ruleMatcher, null);
    }

    StrongTextMasker(MaskStrategyRegistry strategyRegistry, MaskRuleMatcher ruleMatcher,
            Collection<String> fallbackTypes) {
        this.strategyRegistry = strategyRegistry;
        this.keyMatches = ruleMatcher.logKeyMatches(128);
        this.fallbackTypes = normalizedFallbackTypes(fallbackTypes);
    }

    String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            String masked = maskKeyValues(value);
            for (String type : fallbackTypes) {
                masked = maskFallback(masked, type);
            }
            return masked;
        } catch (RuntimeException ex) {
            return value;
        }
    }

    private String maskKeyValues(String value) {
        Matcher matcher = KEY_VALUE.matcher(value);
        StringBuffer masked = new StringBuffer();
        while (matcher.find()) {
            String key = firstPresent(matcher.group(2), matcher.group(3), matcher.group(4));
            String rawValue = matcher.group(6);
            String replacement = matcher.group(1) + matcher.group(5) + maskKeyValue(key, rawValue);
            matcher.appendReplacement(masked, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(masked);
        return masked.toString();
    }

    private String maskKeyValue(String key, String value) {
        RuleMatch match = keyMatches.get(normalizeKey(key));
        if (match == null || match.getAction() != RuleAction.MASK) {
            return value;
        }
        String rawValue = unquote(value);
        if (rawValue.length() > MAX_VALUE_LENGTH) {
            return value;
        }
        Optional<MaskStrategy> strategy = strategyRegistry.find(match.getMaskType());
        if (!strategy.isPresent()) {
            return value;
        }
        String masked = strategy.get().mask(rawValue, MaskContext.builder()
                .maskType(match.getMaskType())
                .scene(MaskScene.UNKNOWN)
                .fieldName(key)
                .rawValue(rawValue)
                .build());
        return requote(value, masked);
    }

    private String maskFallback(String value, String type) {
        Pattern pattern = fallbackPattern(type);
        if (pattern == null) {
            return value;
        }
        Optional<MaskStrategy> strategy = strategyRegistry.find(type);
        if (!strategy.isPresent()) {
            return value;
        }
        Matcher matcher = pattern.matcher(value);
        StringBuffer masked = new StringBuffer();
        while (matcher.find()) {
            String rawValue = matcher.group();
            if (MaskTypes.ID_CARD.equals(type) && !MainlandIdCards.isLikely(rawValue, true)) {
                continue;
            }
            String replacement = strategy.get().mask(rawValue, MaskContext.builder()
                    .maskType(type)
                    .scene(MaskScene.UNKNOWN)
                    .rawValue(rawValue)
                    .build());
            if (!rawValue.equals(replacement)) {
                matcher.appendReplacement(masked, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(masked);
        return masked.toString();
    }

    private static Pattern fallbackPattern(String type) {
        if (MaskTypes.MOBILE.equals(type)) {
            return MOBILE_FALLBACK;
        }
        if (MaskTypes.EMAIL.equals(type)) {
            return EMAIL_FALLBACK;
        }
        if (MaskTypes.ID_CARD.equals(type)) {
            return ID_CARD_FALLBACK;
        }
        if (MaskTypes.BANK_CARD.equals(type)) {
            return BANK_CARD_FALLBACK;
        }
        return null;
    }

    private static List<String> normalizedFallbackTypes(Collection<String> configuredTypes) {
        Collection<String> types = configuredTypes == null || configuredTypes.isEmpty()
                ? Arrays.asList(MaskTypes.MOBILE, MaskTypes.EMAIL, MaskTypes.ID_CARD)
                : configuredTypes;
        List<String> normalized = new ArrayList<String>();
        for (String type : types) {
            String normalizedType = MaskTypes.normalize(type);
            if (!MaskTypes.isUnknown(normalizedType)) {
                normalized.add(normalizedType);
            }
        }
        return normalized;
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
        return key.trim().toLowerCase(java.util.Locale.ENGLISH);
    }
}
