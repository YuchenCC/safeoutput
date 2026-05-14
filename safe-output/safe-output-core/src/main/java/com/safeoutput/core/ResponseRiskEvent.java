package com.safeoutput.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ResponseRiskEvent {

    private final String method;

    private final String path;

    private final String apiKey;

    private final boolean ignored;

    private final String ignoreReason;

    private final boolean failed;

    private final int maskedFieldCount;

    private final Map<String, Integer> maskTypeCounts;

    private final long elapsedNanos;

    public ResponseRiskEvent(String method, String path, boolean ignored, String ignoreReason) {
        this(method, path, null, ignored, ignoreReason, false, 0, Collections.<String, Integer>emptyMap(), 0);
    }

    public ResponseRiskEvent(String method, String path, boolean ignored, String ignoreReason,
            Map<String, Integer> maskTypeCounts, long elapsedNanos) {
        this(method, path, null, ignored, ignoreReason, false, count(maskTypeCounts), maskTypeCounts, elapsedNanos);
    }

    public ResponseRiskEvent(String method, String path, String apiKey, boolean ignored, String ignoreReason,
            boolean failed, int maskedFieldCount, Map<String, Integer> maskTypeCounts, long elapsedNanos) {
        this.method = method;
        this.path = path;
        this.apiKey = apiKey;
        this.ignored = ignored;
        this.ignoreReason = ignoreReason;
        this.failed = failed;
        this.maskedFieldCount = Math.max(0, maskedFieldCount);
        this.maskTypeCounts = immutableCounts(maskTypeCounts);
        this.elapsedNanos = elapsedNanos;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public String getIgnoreReason() {
        return ignoreReason;
    }

    public boolean isFailed() {
        return failed;
    }

    public int getMaskedFieldCount() {
        return maskedFieldCount;
    }

    public Map<String, Integer> getMaskTypeCounts() {
        return maskTypeCounts;
    }

    public long getElapsedNanos() {
        return elapsedNanos;
    }

    private static Map<String, Integer> immutableCounts(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(counts));
    }

    private static int count(Map<String, Integer> counts) {
        int total = 0;
        if (counts != null) {
            for (Integer value : counts.values()) {
                if (value != null && value > 0) {
                    total += value;
                }
            }
        }
        return total;
    }
}
