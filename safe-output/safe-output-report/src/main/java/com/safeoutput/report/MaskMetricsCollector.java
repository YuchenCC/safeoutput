package com.safeoutput.report;

import com.safeoutput.core.MaskScene;
import com.safeoutput.core.MaskType;
import com.safeoutput.core.ResponseRiskEvent;
import com.safeoutput.core.ResponseRiskRecorder;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MaskMetricsCollector implements ResponseRiskRecorder {

    private static final String OVERFLOW_METHOD = "OVERFLOW";
    private static final String OVERFLOW_PATH = "__overflow__";

    private final int maxApiMetrics;
    private final Map<MaskType, Long> maskTypeCounts = new EnumMap<MaskType, Long>(MaskType.class);
    private final Map<String, ApiMaskMetrics> apiMetrics = new LinkedHashMap<String, ApiMaskMetrics>();

    private long totalCount;
    private long responseCount;
    private long logCount;
    private long failureCount;
    private long totalElapsedNanos;
    private long maxElapsedNanos;

    public MaskMetricsCollector(int maxApiMetrics) {
        this.maxApiMetrics = Math.max(1, maxApiMetrics);
    }

    public synchronized void recordMask(MaskScene scene, MaskType type, long elapsedNanos) {
        try {
            if (scene == MaskScene.RESPONSE) {
                responseCount++;
            } else if (scene == MaskScene.LOG) {
                logCount++;
            }
            totalCount++;
            totalElapsedNanos += Math.max(0, elapsedNanos);
            maxElapsedNanos = Math.max(maxElapsedNanos, elapsedNanos);
            long previous = maskTypeCounts.containsKey(type) ? maskTypeCounts.get(type) : 0L;
            maskTypeCounts.put(type, previous + 1);
        } catch (RuntimeException ex) {
            // Metrics must never affect masking flow.
        }
    }

    public synchronized void recordFailure() {
        try {
            failureCount++;
        } catch (RuntimeException ex) {
            // Metrics must never affect masking flow.
        }
    }

    @Override
    public void record(ResponseRiskEvent event) {
        recordApi(event);
    }

    public synchronized void recordApi(ResponseRiskEvent event) {
        try {
            if (event == null) {
                return;
            }
            apiMetric(event).record(event);
        } catch (RuntimeException ex) {
            // Metrics must never affect masking flow.
        }
    }

    public synchronized MaskReport snapshot() {
        long average = totalCount == 0 ? 0 : totalElapsedNanos / totalCount;
        return new MaskReport(totalCount, responseCount, logCount, failureCount, average, maxElapsedNanos,
                maskTypeCounts, new ArrayList<ApiMaskMetrics>(apiMetrics.values()));
    }

    private ApiMaskMetrics apiMetric(ResponseRiskEvent event) {
        String key = key(event.getMethod(), event.getPath());
        if (!apiMetrics.containsKey(key) && apiMetrics.size() >= maxApiMetrics) {
            key = key(OVERFLOW_METHOD, OVERFLOW_PATH);
        }
        ApiMaskMetrics metric = apiMetrics.get(key);
        if (metric == null) {
            metric = new ApiMaskMetrics(method(key), path(key));
            apiMetrics.put(key, metric);
        }
        return metric;
    }

    private static String key(String method, String path) {
        return method + " " + path;
    }

    private static String method(String key) {
        return key.substring(0, key.indexOf(' '));
    }

    private static String path(String key) {
        return key.substring(key.indexOf(' ') + 1);
    }
}
