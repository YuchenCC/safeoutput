package com.safeoutput.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.safeoutput.core.MaskScene;
import com.safeoutput.core.MaskType;
import com.safeoutput.core.MaskTypes;
import com.safeoutput.core.ResponseRiskEvent;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MaskMetricsCollectorTest {

    @Test
    void aggregatesSceneTypeFailureAndLatencyMetrics() {
        MaskMetricsCollector collector = new MaskMetricsCollector(10);

        collector.recordMask(MaskScene.RESPONSE, MaskType.MOBILE, 10);
        collector.recordMask(MaskScene.LOG, MaskType.EMAIL, 20);
        collector.recordUnknownType("mobileM", MaskScene.RESPONSE);
        collector.recordFailure();

        MaskReport report = collector.snapshot();

        assertEquals(2, report.getTotalCount());
        assertEquals(1, report.getResponseCount());
        assertEquals(1, report.getLogCount());
        assertEquals(1, report.getFailureCount());
        assertEquals(15, report.getAverageElapsedNanos());
        assertEquals(20, report.getMaxElapsedNanos());
        assertEquals(1, report.getMaskTypeCounts().get(MaskTypes.MOBILE).longValue());
        assertEquals(1, report.getMaskTypeCounts().get(MaskTypes.EMAIL).longValue());
        assertEquals(1, report.getUnknownTypeCounts().get("mobilem").longValue());
    }

    @Test
    void aggregatesApiRiskAndOverflowWithoutRawValues() {
        MaskMetricsCollector collector = new MaskMetricsCollector(2);
        Map<String, Integer> mobileCounts = new LinkedHashMap<String, Integer>();
        mobileCounts.put(MaskTypes.MOBILE, 5);
        Map<String, Integer> passwordCounts = new LinkedHashMap<String, Integer>();
        passwordCounts.put(MaskTypes.PASSWORD, 1);

        collector.recordApi(new ResponseRiskEvent("GET", "/api/users", false, null, mobileCounts, 30));
        collector.recordApi(new ResponseRiskEvent("GET", "/api/passwords", false, null, passwordCounts, 40));
        collector.recordApi(new ResponseRiskEvent("GET", "/api/raw/mobile", true, "business plaintext lookup"));

        MaskReport report = collector.snapshot();

        assertEquals(3, report.getApiMetrics().size());
        assertEquals(ApiRiskLevel.HIGH, report.getApiMetric("GET", "/api/users").getRiskLevel());
        assertEquals(ApiRiskLevel.CRITICAL, report.getApiMetric("GET", "/api/passwords").getRiskLevel());
        assertEquals(ApiRiskLevel.IGNORED_HIGH, report.getApiMetric("OVERFLOW", "__overflow__").getRiskLevel());
        assertEquals(true, report.getApiMetric("OVERFLOW", "__overflow__").isIgnored());
        assertEquals("business plaintext lookup",
                report.getApiMetric("OVERFLOW", "__overflow__").getIgnoreReason());
        assertFalse(report.toString().contains("13812345678"));
    }

    @Test
    void collectorSwallowsRecorderExceptions() {
        MaskMetricsCollector collector = new MaskMetricsCollector(1);

        collector.record(null);
        collector.recordApi(null);

        assertEquals(0, collector.snapshot().getTotalCount());
    }
}
