package com.safeoutput.report;

import com.safeoutput.core.MaskType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MaskReport {

    private final long totalCount;
    private final long responseCount;
    private final long logCount;
    private final long failureCount;
    private final long averageElapsedNanos;
    private final long maxElapsedNanos;
    private final Map<MaskType, Long> maskTypeCounts;
    private final List<ApiMaskMetrics> apiMetrics;

    MaskReport(long totalCount, long responseCount, long logCount, long failureCount, long averageElapsedNanos,
            long maxElapsedNanos, Map<MaskType, Long> maskTypeCounts, List<ApiMaskMetrics> apiMetrics) {
        this.totalCount = totalCount;
        this.responseCount = responseCount;
        this.logCount = logCount;
        this.failureCount = failureCount;
        this.averageElapsedNanos = averageElapsedNanos;
        this.maxElapsedNanos = maxElapsedNanos;
        this.maskTypeCounts = Collections.unmodifiableMap(new EnumMap<MaskType, Long>(maskTypeCounts));
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

    public long getFailureCount() {
        return failureCount;
    }

    public long getAverageElapsedNanos() {
        return averageElapsedNanos;
    }

    public long getMaxElapsedNanos() {
        return maxElapsedNanos;
    }

    public Map<MaskType, Long> getMaskTypeCounts() {
        return maskTypeCounts;
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
