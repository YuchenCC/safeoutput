package com.safeoutput.report;

import com.safeoutput.core.MaskType;
import com.safeoutput.core.ResponseRiskEvent;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ApiMaskMetrics {

    private final String method;

    private final String path;

    private long hitCount;

    private boolean ignored;

    private String ignoreReason;

    private long totalElapsedNanos;

    private long maxElapsedNanos;

    private final Map<MaskType, Long> maskTypeCounts = new EnumMap<MaskType, Long>(MaskType.class);

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
        totalElapsedNanos += Math.max(0, event.getElapsedNanos());
        maxElapsedNanos = Math.max(maxElapsedNanos, event.getElapsedNanos());
        for (Map.Entry<MaskType, Integer> entry : event.getMaskTypeCounts().entrySet()) {
            long previous = maskTypeCounts.containsKey(entry.getKey()) ? maskTypeCounts.get(entry.getKey()) : 0L;
            maskTypeCounts.put(entry.getKey(), previous + entry.getValue());
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

    public long getAverageElapsedNanos() {
        return hitCount == 0 ? 0 : totalElapsedNanos / hitCount;
    }

    public long getMaxElapsedNanos() {
        return maxElapsedNanos;
    }

    public Map<MaskType, Long> getMaskTypeCounts() {
        return Collections.unmodifiableMap(maskTypeCounts);
    }

    public ApiRiskLevel getRiskLevel() {
        if (ignored) {
            return ApiRiskLevel.IGNORED_HIGH;
        }
        if (has(MaskType.PASSWORD)) {
            return ApiRiskLevel.CRITICAL;
        }
        if (has(MaskType.ID_CARD) || has(MaskType.BANK_CARD) || totalMaskCount() >= 5) {
            return ApiRiskLevel.HIGH;
        }
        if (has(MaskType.MOBILE) || has(MaskType.EMAIL) || has(MaskType.CHINESE_NAME)) {
            return ApiRiskLevel.MEDIUM;
        }
        return totalMaskCount() > 0 ? ApiRiskLevel.LOW : ApiRiskLevel.LOW;
    }

    private boolean has(MaskType type) {
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
