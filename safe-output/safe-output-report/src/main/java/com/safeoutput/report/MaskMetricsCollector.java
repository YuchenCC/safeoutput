package com.safeoutput.report;

import com.safeoutput.core.MaskScene;
import com.safeoutput.core.MaskEventRecorder;
import com.safeoutput.core.MaskType;
import com.safeoutput.core.MaskTypes;
import com.safeoutput.core.ResponseRiskEvent;
import com.safeoutput.core.ResponseRiskRecorder;
import com.safeoutput.core.UnknownTypeRecorder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MaskMetricsCollector implements ResponseRiskRecorder, UnknownTypeRecorder, MaskEventRecorder {

    private static final String OVERFLOW_METHOD = "OVERFLOW";
    private static final String OVERFLOW_PATH = "__overflow__";

    private final int maxApiMetrics;
    private final Map<String, Long> maskTypeCounts = new LinkedHashMap<String, Long>();
    private final Map<String, Long> unknownTypeCounts = new LinkedHashMap<String, Long>();
    private final Map<String, ApiMaskMetrics> apiMetrics = new LinkedHashMap<String, ApiMaskMetrics>();

    private long totalCount;
    private long responseCount;
    private long logCount;
    private long manualCount;
    private long failureCount;
    private long totalElapsedNanos;
    private long maxElapsedNanos;

    public MaskMetricsCollector(int maxApiMetrics) {
        this.maxApiMetrics = Math.max(1, maxApiMetrics);
    }

    public synchronized void recordMask(MaskScene scene, MaskType type, long elapsedNanos) {
        recordMask(scene, MaskTypes.from(type), elapsedNanos);
    }

    public synchronized void recordMask(MaskScene scene, String type, long elapsedNanos) {
        try {
            if (scene == MaskScene.RESPONSE) {
                responseCount++;
            } else if (scene == MaskScene.LOG) {
                logCount++;
            } else if (scene == MaskScene.MANUAL) {
                manualCount++;
            }
            totalCount++;
            totalElapsedNanos += Math.max(0, elapsedNanos);
            maxElapsedNanos = Math.max(maxElapsedNanos, elapsedNanos);
            String normalizedType = MaskTypes.normalize(type);
            long previous = maskTypeCounts.containsKey(normalizedType) ? maskTypeCounts.get(normalizedType) : 0L;
            maskTypeCounts.put(normalizedType, previous + 1);
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
    public synchronized void recordUnknownType(String type, MaskScene scene) {
        try {
            String normalizedType = MaskTypes.normalize(type);
            long previous = unknownTypeCounts.containsKey(normalizedType) ? unknownTypeCounts.get(normalizedType) : 0L;
            unknownTypeCounts.put(normalizedType, previous + 1);
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
        return new MaskReport(totalCount, responseCount, logCount, manualCount, failureCount, average, maxElapsedNanos,
                maskTypeCounts, unknownTypeCounts, new ArrayList<ApiMaskMetrics>(apiMetrics.values()));
    }

    private ApiMaskMetrics apiMetric(ResponseRiskEvent event) {
        String key = key(event.getMethod(), event.getPath());
        if (!apiMetrics.containsKey(key) && apiMetrics.size() >= maxApiMetrics) {
            // 接口维度有上限，超过后聚合到 overflow，避免高基数路径占满内存。
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
