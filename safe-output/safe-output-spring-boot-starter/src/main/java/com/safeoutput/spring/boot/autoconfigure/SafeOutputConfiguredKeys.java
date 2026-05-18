package com.safeoutput.spring.boot.autoconfigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extracts configured rule keys for report-time suggestion filtering.
 */
public final class SafeOutputConfiguredKeys {

    private SafeOutputConfiguredKeys() {
    }

    public static List<String> from(SafeOutputProperties properties) {
        if (properties == null) {
            return Collections.emptyList();
        }
        List<String> keys = new ArrayList<String>();
        for (SafeOutputProperties.RuleProperties rule : properties.getRules()) {
            keys.addAll(rule.getKeys());
        }
        return keys;
    }
}
