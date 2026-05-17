package com.safeoutput.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

@Plugin(name = "SafeOutputMessagePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"safeOutputMsg", "safeOutputMessage"})
public final class SafeOutputMessagePatternConverter extends LogEventPatternConverter {

    private final boolean enabled;

    private final ConverterOptions options;

    private volatile SafeOutputLogMessageMasker cachedMasker;

    private volatile long cachedRuntimeVersion = Long.MIN_VALUE;

    private volatile boolean cachedMaskerResolved;

    private SafeOutputMessagePatternConverter(boolean enabled, ConverterOptions options) {
        super("safeOutputMsg", "safeOutputMsg");
        this.enabled = enabled;
        this.options = options;
    }

    public static SafeOutputMessagePatternConverter newInstance(String[] options) {
        try {
            ConverterOptions parsedOptions = ConverterOptions.parse(options);
            return new SafeOutputMessagePatternConverter(parsedOptions.enabled, parsedOptions);
        } catch (RuntimeException ex) {
            // converter 初始化失败时禁用脱敏，避免日志配置问题影响业务启动或日志输出。
            return new SafeOutputMessagePatternConverter(false, new ConverterOptions());
        }
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        String message = event == null || event.getMessage() == null
                ? ""
                : event.getMessage().getFormattedMessage();
        if (!enabled) {
            toAppendTo.append(message);
            return;
        }
        try {
            SafeOutputLogMessageMasker masker = currentMasker();
            if (masker == null) {
                toAppendTo.append(message);
                return;
            }
            toAppendTo.append(masker.mask(message));
        } catch (RuntimeException ex) {
            toAppendTo.append(message);
        }
    }

    private SafeOutputLogMessageMasker currentMasker() {
        long runtimeVersion = SafeOutputLog4j2Runtime.version();
        if (cachedMaskerResolved && cachedRuntimeVersion == runtimeVersion) {
            return cachedMasker;
        }
        synchronized (this) {
            runtimeVersion = SafeOutputLog4j2Runtime.version();
            if (cachedMaskerResolved && cachedRuntimeVersion == runtimeVersion) {
                return cachedMasker;
            }
            cachedMasker = SafeOutputLog4j2Runtime.createMasker(options.enabled,
                    options.maxMessageLength, options.maxValueLength, options.regexFallback,
                    options.idCardCheckCode, options.keyValueRuleEnabled, options.maxRuleKeys);
            cachedRuntimeVersion = runtimeVersion;
            cachedMaskerResolved = true;
            return cachedMasker;
        }
    }

    private static final class ConverterOptions {

        private boolean enabled = true;

        private int maxMessageLength = 5000;

        private int maxValueLength = 300;

        private boolean regexFallback = true;

        private boolean idCardCheckCode = true;

        private boolean keyValueRuleEnabled = true;

        private int maxRuleKeys = 128;

        private static ConverterOptions parse(String[] options) {
            ConverterOptions parsedOptions = new ConverterOptions();
            if (options == null) {
                return parsedOptions;
            }
            for (String option : options) {
                if (option == null) {
                    continue;
                }
                String[] entries = option.split(",");
                for (String entry : entries) {
                    parsedOptions.apply(entry);
                }
            }
            return parsedOptions;
        }

        private void apply(String entry) {
            String[] keyValue = entry.trim().split("=", 2);
            if (keyValue.length != 2) {
                return;
            }
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();
            if ("enabled".equalsIgnoreCase(key)) {
                enabled = Boolean.parseBoolean(value);
            } else if ("keyValueRuleEnabled".equalsIgnoreCase(key)) {
                keyValueRuleEnabled = Boolean.parseBoolean(value);
            } else if ("regexFallback".equalsIgnoreCase(key)) {
                regexFallback = Boolean.parseBoolean(value);
            } else if ("idCardCheckCode".equalsIgnoreCase(key)) {
                idCardCheckCode = Boolean.parseBoolean(value);
            } else if ("maxMessageLength".equalsIgnoreCase(key)) {
                maxMessageLength = parsePositiveInt(value, maxMessageLength);
            } else if ("maxValueLength".equalsIgnoreCase(key)) {
                maxValueLength = parsePositiveInt(value, maxValueLength);
            } else if ("maxRuleKeys".equalsIgnoreCase(key)) {
                maxRuleKeys = parsePositiveInt(value, maxRuleKeys);
            }
        }

        private static int parsePositiveInt(String value, int fallback) {
            try {
                int parsed = Integer.parseInt(value);
                return parsed > 0 ? parsed : fallback;
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }
    }
}
