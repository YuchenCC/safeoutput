package com.safeoutput.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MaskingResult {

    private final Object value;

    private final Map<String, Integer> maskTypeCounts;

    private final int maskedFieldCount;

    MaskingResult(Object value, Map<String, Integer> maskTypeCounts, int maskedFieldCount) {
        this.value = value;
        this.maskTypeCounts = Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(maskTypeCounts));
        this.maskedFieldCount = maskedFieldCount;
    }

    public Object getValue() {
        return value;
    }

    public Map<String, Integer> getMaskTypeCounts() {
        return maskTypeCounts;
    }

    public int getMaskedFieldCount() {
        return maskedFieldCount;
    }
}
