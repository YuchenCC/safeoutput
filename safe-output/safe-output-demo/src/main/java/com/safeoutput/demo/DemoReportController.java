package com.safeoutput.demo;

import com.safeoutput.core.LogRuleSuggestionEvent;
import com.safeoutput.core.MaskTypes;
import com.safeoutput.report.LogRuleSuggestionAnalyzer;
import com.safeoutput.report.LogRuleSuggestionReport;
import com.safeoutput.report.MaskMetricsCollector;
import com.safeoutput.report.MaskReport;
import com.safeoutput.report.MaskReportExporter;
import com.safeoutput.report.ResponseRiskAnalysis;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoReportController {

    private final MaskReportExporter exporter;

    private final MaskMetricsCollector metricsCollector;

    private boolean logSuggestionsSeeded;

    public DemoReportController(MaskReportExporter exporter, MaskMetricsCollector metricsCollector) {
        this.exporter = exporter;
        this.metricsCollector = metricsCollector;
    }

    @GetMapping("/demo/report/snapshot")
    public MaskReport snapshot() {
        return metricsCollector.snapshot();
    }

    @GetMapping("/demo/report/export")
    public Map<String, String> export() {
        Path path = exporter.exportNow();
        return Collections.singletonMap("path", path == null ? "" : path.toString());
    }

    @GetMapping("/demo/report/response-risk")
    public Map<String, Object> responseRisk() {
        ResponseRiskAnalysis analysis = metricsCollector.snapshot().getResponseRiskAnalysis();
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("responseRiskSummary", analysis.getResponseRiskSummary());
        response.put("topRiskApis", analysis.getTopRiskApis());
        response.put("ignoredRiskApis", analysis.getIgnoredRiskApis());
        return response;
    }

    @GetMapping("/demo/report/log-suggestions")
    public Map<String, Object> logSuggestions() {
        seedLogSuggestions();
        LogRuleSuggestionReport report = new LogRuleSuggestionAnalyzer().analyze(
                metricsCollector.snapshotSuggestions(), Collections.<String>emptyList());
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("logRuleSuggestions", report.getLogRuleSuggestions());
        response.put("configSnippet", report.getConfigSnippet());
        return response;
    }

    private synchronized void seedLogSuggestions() {
        if (logSuggestionsSeeded) {
            return;
        }
        record("phoneNo", MaskTypes.MOBILE, 5);
        record("certNum", MaskTypes.ID_CARD, 2);
        record("mailAddr", MaskTypes.EMAIL, 2);
        logSuggestionsSeeded = true;
    }

    private void record(String key, String type, int count) {
        String normalizedKey = key.toLowerCase(java.util.Locale.ENGLISH);
        for (int i = 0; i < count; i++) {
            // Demo 只写入脱敏后的 evidence，避免报告接口携带敏感原文。
            metricsCollector.record(new LogRuleSuggestionEvent(normalizedKey, type,
                    normalizedKey + "=<" + MaskTypes.normalize(type) + ">", System.currentTimeMillis()));
        }
    }
}
