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
        collector.recordMask(MaskScene.MANUAL, MaskType.ID_CARD, 30);
        collector.recordUnknownType("mobileM", MaskScene.RESPONSE);
        collector.recordFailure();

        MaskReport report = collector.snapshot();

        assertEquals(3, report.getTotalCount());
        assertEquals(1, report.getResponseCount());
        assertEquals(1, report.getLogCount());
        assertEquals(1, report.getManualCount());
        assertEquals(1, report.getFailureCount());
        assertEquals(20, report.getAverageElapsedNanos());
        assertEquals(30, report.getMaxElapsedNanos());
        assertEquals(1, report.getMaskTypeCounts().get(MaskTypes.MOBILE).longValue());
        assertEquals(1, report.getMaskTypeCounts().get(MaskTypes.EMAIL).longValue());
        assertEquals(1, report.getMaskTypeCounts().get(MaskTypes.ID_CARD).longValue());
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
    void aggregatesResponseApiLatencyFieldCountsSlowMasksAndStableKey() {
        MaskMetricsCollector collector = new MaskMetricsCollector(10);
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        counts.put(MaskTypes.MOBILE, 2);
        counts.put(MaskTypes.ID_CARD, 1);

        collector.recordApi(new ResponseRiskEvent("GET", "/customers/123", "/customers/{id}", false, null, false,
                3, counts, 60000000L));
        collector.recordApi(new ResponseRiskEvent("GET", "/customers/456", "/customers/{id}", false, null, true,
                0, new LinkedHashMap<String, Integer>(), 10L));

        ApiMaskMetrics metric = collector.snapshot().getApiMetric("GET", "/customers/{id}");

        assertEquals(2, metric.getHitCount());
        assertEquals(30000005L, metric.getAverageElapsedNanos());
        assertEquals(60000000L, metric.getMaxElapsedNanos());
        assertEquals(1, metric.getSlowMaskCount());
        assertEquals(1, metric.getFailureCount());
        assertEquals(3, metric.getMaskedFieldCount());
        assertEquals(2, metric.getMaskTypeCounts().get(MaskTypes.MOBILE).longValue());
        assertEquals(1, metric.getMaskTypeCounts().get(MaskTypes.ID_CARD).longValue());
        assertEquals(null, collector.snapshot().getApiMetric("GET", "/customers/123"));
    }

    @Test
    void ignoredApiStillRecordsRiskBaseData() {
        MaskMetricsCollector collector = new MaskMetricsCollector(10);

        collector.recordApi(new ResponseRiskEvent("GET", "/demo/ignored", "/demo/ignored", true,
                "demo plaintext endpoint", false, 0, new LinkedHashMap<String, Integer>(), 0));

        ApiMaskMetrics metric = collector.snapshot().getApiMetric("GET", "/demo/ignored");
        assertEquals(true, metric.isIgnored());
        assertEquals("demo plaintext endpoint", metric.getIgnoreReason());
        assertEquals(0, metric.getMaskedFieldCount());
    }

    @Test
    void responseRiskAnalysisReportsRiskReasonsAdviceAndPerformanceSeparately() {
        MaskMetricsCollector collector = new MaskMetricsCollector(10);
        Map<String, Integer> riskyCounts = new LinkedHashMap<String, Integer>();
        riskyCounts.put(MaskTypes.ID_CARD, 1);
        riskyCounts.put(MaskTypes.BANK_CARD, 1);
        riskyCounts.put(MaskTypes.PASSWORD, 1);
        collector.recordApi(new ResponseRiskEvent("GET", "/risky", "/risky", false, null, false, 6,
                riskyCounts, 60000000L));
        collector.recordApi(new ResponseRiskEvent("GET", "/ignored", "/ignored", true, "business plaintext", false,
                0, new LinkedHashMap<String, Integer>(), 0));

        ResponseRiskAnalysis analysis = collector.snapshot().getResponseRiskAnalysis();
        ResponseRiskApiProfile top = analysis.getTopRiskApis().get(0);

        assertEquals(2, analysis.getResponseRiskSummary().getApiCount());
        assertEquals(2, analysis.getResponseRiskSummary().getHighRiskApiCount());
        assertEquals(1, analysis.getResponseRiskSummary().getIgnoredApiCount());
        assertEquals(1, analysis.getResponseRiskSummary().getSlowApiCount());
        assertEquals(ApiRiskLevel.CRITICAL, top.getRiskLevel());
        assertEquals(true, top.getRiskReasons().contains("ID_CARD"));
        assertEquals(true, top.getRiskReasons().contains("BANK_CARD"));
        assertEquals(true, top.getRiskReasons().contains("PASSWORD"));
        assertEquals(true, top.getRiskReasons().contains("HIGH_FIELD_COUNT"));
        assertEquals(1, top.getPerformanceProfile().getSlowMaskCount());
        assertEquals(true, top.getPerformanceProfile().getWarnings().contains("SLOW_MASKING"));
        assertEquals("business plaintext", analysis.getIgnoredRiskApis().get(0).getIgnoreReason());
    }

    @Test
    void collectorSwallowsRecorderExceptions() {
        MaskMetricsCollector collector = new MaskMetricsCollector(1);

        collector.record(null);
        collector.recordApi(null);

        assertEquals(0, collector.snapshot().getTotalCount());
    }
}
