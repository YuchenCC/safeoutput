package com.safeoutput.log4j2;

import com.safeoutput.core.LogRuleSuggestionCollector;
import com.safeoutput.core.MaskEventRecorder;
import com.safeoutput.core.MaskRuleMatcher;
import com.safeoutput.core.MaskStrategyRegistry;
import com.safeoutput.core.UnknownTypeRecorder;

/**
 * Runtime bridge used by the Spring Boot starter to supply configured log masking dependencies.
 */
public final class SafeOutputLog4j2Runtime {

    private static volatile Configuration configuration;

    private static volatile long version;

    private SafeOutputLog4j2Runtime() {
    }

    public static void configure(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry,
            boolean enabled, int maxMessageLength, int maxValueLength, boolean regexFallbackEnabled,
            boolean idCardCheckCodeEnabled, boolean keyValueRuleEnabled, int maxRuleKeys) {
        configure(ruleMatcher, strategyRegistry, enabled, maxMessageLength, maxValueLength, regexFallbackEnabled,
                idCardCheckCodeEnabled, keyValueRuleEnabled, maxRuleKeys, null);
    }

    public static void configure(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry,
            boolean enabled, int maxMessageLength, int maxValueLength, boolean regexFallbackEnabled,
            boolean idCardCheckCodeEnabled, boolean keyValueRuleEnabled, int maxRuleKeys,
            UnknownTypeRecorder unknownTypeRecorder) {
        configure(ruleMatcher, strategyRegistry, enabled, maxMessageLength, maxValueLength, regexFallbackEnabled,
                idCardCheckCodeEnabled, keyValueRuleEnabled, maxRuleKeys, unknownTypeRecorder, null, null);
    }

    public static void configure(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry,
            boolean enabled, int maxMessageLength, int maxValueLength, boolean regexFallbackEnabled,
            boolean idCardCheckCodeEnabled, boolean keyValueRuleEnabled, int maxRuleKeys,
            UnknownTypeRecorder unknownTypeRecorder, LogRuleSuggestionCollector suggestionCollector,
            MaskEventRecorder maskEventRecorder) {
        if (ruleMatcher == null || strategyRegistry == null) {
            reset();
            return;
        }
        configuration = new Configuration(ruleMatcher, strategyRegistry, enabled, maxMessageLength, maxValueLength,
                regexFallbackEnabled, idCardCheckCodeEnabled, keyValueRuleEnabled, maxRuleKeys, unknownTypeRecorder,
                suggestionCollector, maskEventRecorder);
        version++;
    }

    public static void reset() {
        configuration = null;
        version++;
    }

    static long version() {
        return version;
    }

    static SafeOutputLogMessageMasker createMasker(boolean optionEnabled, int optionMaxMessageLength,
            int optionMaxValueLength, boolean optionRegexFallback, boolean optionIdCardCheckCode,
            boolean optionKeyValueRuleEnabled, int optionMaxRuleKeys) {
        Configuration current = configuration;
        if (current == null) {
            return new SafeOutputLogMessageMasker(MaskRuleMatcher.withDefaultRules(),
                    MaskStrategyRegistry.withBuiltIns(), optionMaxMessageLength, optionMaxValueLength,
                    optionRegexFallback, optionIdCardCheckCode, optionKeyValueRuleEnabled, optionMaxRuleKeys);
        }
        if (!optionEnabled || !current.enabled) {
            return null;
        }
        return new SafeOutputLogMessageMasker(current.ruleMatcher, current.strategyRegistry,
                current.maxMessageLength, current.maxValueLength, current.regexFallbackEnabled,
                current.idCardCheckCodeEnabled, current.keyValueRuleEnabled, current.maxRuleKeys,
                current.suggestionCollector, current.unknownTypeRecorder, current.maskEventRecorder);
    }

    private static final class Configuration {

        private final MaskRuleMatcher ruleMatcher;

        private final MaskStrategyRegistry strategyRegistry;

        private final boolean enabled;

        private final int maxMessageLength;

        private final int maxValueLength;

        private final boolean regexFallbackEnabled;

        private final boolean idCardCheckCodeEnabled;

        private final boolean keyValueRuleEnabled;

        private final int maxRuleKeys;

        private final UnknownTypeRecorder unknownTypeRecorder;

        private final LogRuleSuggestionCollector suggestionCollector;

        private final MaskEventRecorder maskEventRecorder;

        private Configuration(MaskRuleMatcher ruleMatcher, MaskStrategyRegistry strategyRegistry, boolean enabled,
                int maxMessageLength, int maxValueLength, boolean regexFallbackEnabled,
                boolean idCardCheckCodeEnabled, boolean keyValueRuleEnabled, int maxRuleKeys,
                UnknownTypeRecorder unknownTypeRecorder, LogRuleSuggestionCollector suggestionCollector,
                MaskEventRecorder maskEventRecorder) {
            this.ruleMatcher = ruleMatcher;
            this.strategyRegistry = strategyRegistry;
            this.enabled = enabled;
            this.maxMessageLength = maxMessageLength;
            this.maxValueLength = maxValueLength;
            this.regexFallbackEnabled = regexFallbackEnabled;
            this.idCardCheckCodeEnabled = idCardCheckCodeEnabled;
            this.keyValueRuleEnabled = keyValueRuleEnabled;
            this.maxRuleKeys = maxRuleKeys;
            this.unknownTypeRecorder = unknownTypeRecorder;
            this.suggestionCollector = suggestionCollector;
            this.maskEventRecorder = maskEventRecorder;
        }
    }
}
