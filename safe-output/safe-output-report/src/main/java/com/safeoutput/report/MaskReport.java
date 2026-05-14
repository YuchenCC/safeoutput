package com.safeoutput.report;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MaskReport {

    private final long totalCount;
    private final long responseCount;
    private final long logCount;
    private final long manualCount;
    private final long failureCount;
    private final long averageElapsedNanos;
    private final long maxElapsedNanos;
    private final Map<String, Long> maskTypeCounts;
    private final Map<String, Long> unknownTypeCounts;
    private final List<ApiMaskMetrics> apiMetrics;

    MaskReport(long totalCount, long responseCount, long logCount, long manualCount, long failureCount,
            long averageElapsedNanos, long maxElapsedNanos, Map<String, Long> maskTypeCounts,
            Map<String, Long> unknownTypeCounts, List<ApiMaskMetrics> apiMetrics) {
        this.totalCount = totalCount;
        this.responseCount = responseCount;
        this.logCount = logCount;
        this.manualCount = manualCount;
        this.failureCount = failureCount;
        this.averageElapsedNanos = averageElapsedNanos;
        this.maxElapsedNanos = maxElapsedNanos;
        this.maskTypeCounts = Collections.unmodifiableMap(new LinkedHashMap<String, Long>(maskTypeCounts));
        this.unknownTypeCounts = Collections.unmodifiableMap(new LinkedHashMap<String, Long>(unknownTypeCounts));
        this.apiMetrics = Collections.unmodifiableList(apiMetrics);
    }

    public long getTotalCount() {
        return totalCount;
    }

    public long getResponseCount() {
        return responseCount;
    }

    public long getLogCount() {
        return logCount;
    }

    public long getManualCount() {
        return manualCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public long getAverageElapsedNanos() {
        return averageElapsedNanos;
    }

    public long getMaxElapsedNanos() {
        return maxElapsedNanos;
    }

    public Map<String, Long> getMaskTypeCounts() {
        return maskTypeCounts;
    }

    public Map<String, Long> getUnknownTypeCounts() {
        return unknownTypeCounts;
    }

    public List<ApiMaskMetrics> getApiMetrics() {
        return apiMetrics;
    }

    public ApiMaskMetrics getApiMetric(String method, String path) {
        for (ApiMaskMetrics metric : apiMetrics) {
            if (metric.getMethod().equals(method) && metric.getPath().equals(path)) {
                return metric;
            }
        }
        return null;
    }
}
