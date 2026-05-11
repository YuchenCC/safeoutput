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
            return new SafeOutputMessagePatternConverter(isEnabled(options), new SafeOutputLogMessageMasker());
        } catch (RuntimeException ex) {
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

    private static boolean isEnabled(String[] options) {
        if (options == null) {
            return true;
        }
        for (String option : options) {
            if (option != null && "enabled=false".equalsIgnoreCase(option.trim())) {
                return false;
            }
        }
        return true;
    }
}
