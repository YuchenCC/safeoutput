package com.safeoutput.demo;

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

    public DemoReportController(MaskReportExporter exporter, MaskMetricsCollector metricsCollector) {
        this.exporter = exporter;
        this.metricsCollector = metricsCollector;
    }

    @GetMapping("/demo/report/snapshot")
    public MaskReport snapshot() {
        return metricsCollector.snapshot();
    }

    @GetMapping("/demo/report/dashboard")
    public Map<String, Object> dashboard() {
        MaskReport report = metricsCollector.snapshot();
        ResponseRiskAnalysis riskAnalysis = report.getResponseRiskAnalysis();
        LogRuleSuggestionReport suggestionReport = new LogRuleSuggestionAnalyzer().analyze(
                metricsCollector.snapshotSuggestions(), Collections.<String>emptyList());

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("totalCount", report.getTotalCount());
        result.put("responseCount", report.getResponseCount());
        result.put("logCount", report.getLogCount());
        result.put("manualCount", report.getManualCount());
        result.put("highRiskApiCount", riskAnalysis.getResponseRiskSummary().getHighRiskApiCount());
        result.put("suggestionCount", suggestionReport.getLogRuleSuggestions().size());
        result.put("averageElapsedNanos", report.getAverageElapsedNanos());
        result.put("maskTypeCounts", report.getMaskTypeCounts());
        result.put("topRiskApis", riskAnalysis.getTopRiskApis());

        Map<String, Long> sceneTrend = new LinkedHashMap<String, Long>();
        sceneTrend.put("response", report.getResponseCount());
        sceneTrend.put("log", report.getLogCount());
        sceneTrend.put("manual", report.getManualCount());
        result.put("sceneTrend", sceneTrend);

        return result;
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
        LogRuleSuggestionReport report = new LogRuleSuggestionAnalyzer().analyze(
                metricsCollector.snapshotSuggestions(), Collections.<String>emptyList());
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("logRuleSuggestions", report.getLogRuleSuggestions());
        response.put("configSnippet", report.getConfigSnippet());
        return response;
    }
}
