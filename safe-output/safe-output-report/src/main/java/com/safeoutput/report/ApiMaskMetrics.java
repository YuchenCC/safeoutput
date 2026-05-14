package com.safeoutput.report;

import com.safeoutput.core.MaskTypes;
import com.safeoutput.core.ResponseRiskEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiMaskMetrics {

    private static final long SLOW_MASK_NANOS = 50000000L;

    private final String method;

    private final String path;

    private long hitCount;

    private boolean ignored;

    private String ignoreReason;

    private long failureCount;

    private long maskedFieldCount;

    private long totalElapsedNanos;

    private long maxElapsedNanos;

    private long slowMaskCount;

    private final Map<String, Long> maskTypeCounts = new LinkedHashMap<String, Long>();

    ApiMaskMetrics(String method, String path) {
        this.method = method;
        this.path = path;
    }

    void record(ResponseRiskEvent event) {
        hitCount++;
        ignored = ignored || event.isIgnored();
        if (event.getIgnoreReason() != null) {
            ignoreReason = event.getIgnoreReason();
        }
        if (event.isFailed()) {
            failureCount++;
        }
        maskedFieldCount += event.getMaskedFieldCount();
        totalElapsedNanos += Math.max(0, event.getElapsedNanos());
        maxElapsedNanos = Math.max(maxElapsedNanos, event.getElapsedNanos());
        if (event.getElapsedNanos() >= SLOW_MASK_NANOS) {
            slowMaskCount++;
        }
        for (Map.Entry<String, Integer> entry : event.getMaskTypeCounts().entrySet()) {
            String type = MaskTypes.normalize(entry.getKey());
            long previous = maskTypeCounts.containsKey(type) ? maskTypeCounts.get(type) : 0L;
            maskTypeCounts.put(type, previous + entry.getValue());
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public long getHitCount() {
        return hitCount;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public String getIgnoreReason() {
        return ignoreReason;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public long getMaskedFieldCount() {
        return maskedFieldCount;
    }

    public long getAverageElapsedNanos() {
        return hitCount == 0 ? 0 : totalElapsedNanos / hitCount;
    }

    public long getMaxElapsedNanos() {
        return maxElapsedNanos;
    }

    public long getSlowMaskCount() {
        return slowMaskCount;
    }

    public Map<String, Long> getMaskTypeCounts() {
        return Collections.unmodifiableMap(maskTypeCounts);
    }

    public ApiRiskLevel getRiskLevel() {
        if (ignored) {
            return ApiRiskLevel.IGNORED_HIGH;
        }
        if (has(MaskTypes.PASSWORD)) {
            return ApiRiskLevel.CRITICAL;
        }
        if (has(MaskTypes.ID_CARD) || has(MaskTypes.BANK_CARD) || totalMaskCount() >= 5) {
            return ApiRiskLevel.HIGH;
        }
        if (has(MaskTypes.MOBILE) || has(MaskTypes.EMAIL) || has(MaskTypes.CHINESE_NAME)) {
            return ApiRiskLevel.MEDIUM;
        }
        return totalMaskCount() > 0 ? ApiRiskLevel.LOW : ApiRiskLevel.LOW;
    }

    private boolean has(String type) {
        return maskTypeCounts.containsKey(type) && maskTypeCounts.get(type) > 0;
    }

    private long totalMaskCount() {
        long count = 0;
        for (Long value : maskTypeCounts.values()) {
            count += value;
        }
        return count;
    }
}
