package com.safeoutput.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ResponseRiskEvent {

    private final String method;

    private final String path;

    private final boolean ignored;

    private final String ignoreReason;

    private final Map<String, Integer> maskTypeCounts;

    private final long elapsedNanos;

    public ResponseRiskEvent(String method, String path, boolean ignored, String ignoreReason) {
        this(method, path, ignored, ignoreReason, Collections.<String, Integer>emptyMap(), 0);
    }

    public ResponseRiskEvent(String method, String path, boolean ignored, String ignoreReason,
            Map<String, Integer> maskTypeCounts, long elapsedNanos) {
        this.method = method;
        this.path = path;
        this.ignored = ignored;
        this.ignoreReason = ignoreReason;
        this.maskTypeCounts = immutableCounts(maskTypeCounts);
        this.elapsedNanos = elapsedNanos;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public String getIgnoreReason() {
        return ignoreReason;
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
}
