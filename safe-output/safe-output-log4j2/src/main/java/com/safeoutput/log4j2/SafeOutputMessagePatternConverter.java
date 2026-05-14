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

    private final SafeOutputLogMessageMasker masker;

    private SafeOutputMessagePatternConverter(boolean enabled, SafeOutputLogMessageMasker masker) {
        super("safeOutputMsg", "safeOutputMsg");
        this.enabled = enabled;
        this.masker = masker;
    }

    public static SafeOutputMessagePatternConverter newInstance(String[] options) {
        try {
            ConverterOptions parsedOptions = ConverterOptions.parse(options);
            return new SafeOutputMessagePatternConverter(parsedOptions.enabled,
                    new SafeOutputLogMessageMasker(parsedOptions.maxMessageLength, parsedOptions.maxValueLength,
                            parsedOptions.regexFallback));
        } catch (RuntimeException ex) {
            // converter 初始化失败时禁用脱敏，避免日志配置问题影响业务启动或日志输出。
            return new SafeOutputMessagePatternConverter(false, null);
        }
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        String message = event == null || event.getMessage() == null
                ? ""
                : event.getMessage().getFormattedMessage();
        if (!enabled || masker == null) {
            toAppendTo.append(message);
            return;
        }
        try {
            toAppendTo.append(masker.mask(message));
        } catch (RuntimeException ex) {
            toAppendTo.append(message);
        }
    }

    private static final class ConverterOptions {

        private boolean enabled = true;

        private int maxMessageLength = 5000;

        private int maxValueLength = 300;

        private boolean regexFallback = true;

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
            } else if ("regexFallback".equalsIgnoreCase(key)) {
                regexFallback = Boolean.parseBoolean(value);
            } else if ("maxMessageLength".equalsIgnoreCase(key)) {
                maxMessageLength = parsePositiveInt(value, maxMessageLength);
            } else if ("maxValueLength".equalsIgnoreCase(key)) {
                maxValueLength = parsePositiveInt(value, maxValueLength);
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
