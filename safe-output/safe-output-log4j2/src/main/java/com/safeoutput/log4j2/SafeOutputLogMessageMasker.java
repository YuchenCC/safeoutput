package com.safeoutput.log4j2;

import com.safeoutput.core.MaskContext;
import com.safeoutput.core.MaskRuleMatcher;
import com.safeoutput.core.MaskScene;
import com.safeoutput.core.MaskStrategy;
import com.safeoutput.core.MaskStrategyRegistry;
import com.safeoutput.core.RuleAction;
import com.safeoutput.core.RuleMatch;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SafeOutputLogMessageMasker {

    private static final Pattern KEY_VALUE = Pattern.compile(
            "(\"([A-Za-z][A-Za-z0-9_-]*)\"|'([A-Za-z][A-Za-z0-9_-]*)'|([A-Za-z][A-Za-z0-9_-]*))"
                    + "(\\s*[:=]\\s*)(\"[^\"]*\"|'[^']*'|[^\\s,}]+)");

    private final MaskRuleMatcher ruleMatcher;

    private final MaskStrategyRegistry strategyRegistry;

    SafeOutputLogMessageMasker() {
        this(MaskRuleMatcher.withDefaultRules(), MaskStrategyRegistry.withBuiltIns());
    }

    SafeOutputLogMessageMasker(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry) {
        this.ruleMatcher = ruleMatcher;
        this.strategyRegistry = strategyRegistry;
    }

    String mask(String message) {
        if (message == null || message.isEmpty()) {
            return message;
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
        return masked.toString();
    }

    private String maskValue(String key, String value) {
        Optional<RuleMatch> match = ruleMatcher.match(key, null);
        if (!match.isPresent() || match.get().getAction() != RuleAction.MASK) {
            return value;
        }
        String rawValue = unquote(value);
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
