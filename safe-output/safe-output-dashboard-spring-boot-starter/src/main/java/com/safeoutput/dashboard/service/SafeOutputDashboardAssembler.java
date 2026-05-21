package com.safeoutput.dashboard.service;

import com.safeoutput.report.LogRuleSuggestionReport;
import com.safeoutput.report.MaskReport;
import com.safeoutput.report.ResponseRiskAnalysis;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SafeOutputDashboardAssembler {

    public Map<String, Object> realtime(MaskReport report, LogRuleSuggestionReport suggestionReport) {
        ResponseRiskAnalysis riskAnalysis = report.getResponseRiskAnalysis();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("totalCount", report.getTotalCount());
        result.put("responseCount", report.getResponseCount());
        result.put("logCount", report.getLogCount());
        result.put("manualCount", report.getManualCount());
        result.put("failureCount", report.getFailureCount());
        result.put("averageElapsedNanos", report.getAverageElapsedNanos());
        result.put("maxElapsedNanos", report.getMaxElapsedNanos());
        result.put("maskTypeCounts", report.getMaskTypeCounts());
        result.put("unknownTypeCounts", report.getUnknownTypeCounts());
        result.put("apiCount", riskAnalysis.getResponseRiskSummary().getApiCount());
        result.put("highRiskApiCount", riskAnalysis.getResponseRiskSummary().getHighRiskApiCount());
        result.put("ignoredApiCount", riskAnalysis.getResponseRiskSummary().getIgnoredApiCount());
        result.put("slowApiCount", riskAnalysis.getResponseRiskSummary().getSlowApiCount());
        result.put("topRiskApis", riskAnalysis.getTopRiskApis());
        result.put("ignoredRiskApis", riskAnalysis.getIgnoredRiskApis());
        result.put("suggestionCount", suggestionReport == null ? 0 : suggestionReport.getLogRuleSuggestions().size());
        result.put("logRuleSuggestions", suggestionReport == null
                ? Collections.emptyList()
                : suggestionReport.getLogRuleSuggestions());
        result.put("sceneTrend", sceneTrend(report));
        return result;
    }

    public Map<String, Object> responseRisk(MaskReport report) {
        ResponseRiskAnalysis analysis = report.getResponseRiskAnalysis();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("responseRiskSummary", analysis.getResponseRiskSummary());
        result.put("topRiskApis", analysis.getTopRiskApis());
        result.put("ignoredRiskApis", analysis.getIgnoredRiskApis());
        return result;
    }

    public Map<String, Object> historical(String filename, Map<String, Object> report) {
        Map<String, Object> dashboard = new LinkedHashMap<String, Object>();
        dashboard.put("filename", filename);
        dashboard.put("totalCount", report.get("totalCount"));
        dashboard.put("responseCount", report.get("responseCount"));
        dashboard.put("logCount", report.get("logCount"));
        dashboard.put("manualCount", report.get("manualCount"));
        dashboard.put("failureCount", report.get("failureCount"));
        dashboard.put("averageElapsedNanos", report.get("averageElapsedNanos"));
        dashboard.put("maxElapsedNanos", report.get("maxElapsedNanos"));
        dashboard.put("maskTypeCounts", report.get("maskTypeCounts"));
        addRiskSummary(dashboard, report);
        dashboard.put("topRiskApis", enrichRiskApis(report.get("topRiskApis"), report.get("apiMetrics")));
        dashboard.put("ignoredRiskApis", enrichRiskApis(report.get("ignoredRiskApis"), report.get("apiMetrics")));
        dashboard.put("logRuleSuggestions", report.get("logRuleSuggestions"));
        dashboard.put("configSnippet", report.get("configSnippet"));
        return dashboard;
    }

    private static Map<String, Long> sceneTrend(MaskReport report) {
        Map<String, Long> sceneTrend = new LinkedHashMap<String, Long>();
        sceneTrend.put("response", report.getResponseCount());
        sceneTrend.put("log", report.getLogCount());
        sceneTrend.put("manual", report.getManualCount());
        return sceneTrend;
    }

    @SuppressWarnings("unchecked")
    private static void addRiskSummary(Map<String, Object> dashboard, Map<String, Object> report) {
        Object summary = report.get("responseRiskSummary");
        if (!(summary instanceof Map)) {
            putIfPresent(dashboard, "apiCount", report.get("apiCount"));
            putIfPresent(dashboard, "highRiskApiCount", report.get("highRiskApiCount"));
            putIfPresent(dashboard, "ignoredApiCount", report.get("ignoredApiCount"));
            putIfPresent(dashboard, "slowApiCount", report.get("slowApiCount"));
            return;
        }
        Map<String, Object> values = (Map<String, Object>) summary;
        dashboard.put("apiCount", values.get("apiCount"));
        dashboard.put("highRiskApiCount", values.get("highRiskApiCount"));
        dashboard.put("ignoredApiCount", values.get("ignoredApiCount"));
        dashboard.put("slowApiCount", values.get("slowApiCount"));
    }

    private static void putIfPresent(Map<String, Object> values, String key, Object value) {
        if (value != null) {
            values.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object enrichRiskApis(Object apis, Object apiMetrics) {
        if (!(apis instanceof Iterable) || !(apiMetrics instanceof Iterable)) {
            return apis;
        }
        Map<String, Map<String, Object>> metrics = new LinkedHashMap<String, Map<String, Object>>();
        for (Object metric : (Iterable<Object>) apiMetrics) {
            if (metric instanceof Map) {
                Map<String, Object> values = (Map<String, Object>) metric;
                metrics.put(apiKey(values), values);
            }
        }
        for (Object api : (Iterable<Object>) apis) {
            if (api instanceof Map) {
                Map<String, Object> values = (Map<String, Object>) api;
                Map<String, Object> metric = metrics.get(apiKey(values));
                if (metric != null) {
                    putIfMissing(values, "hitCount", metric.get("hitCount"));
                    putIfMissing(values, "maskedFieldCount", metric.get("maskedFieldCount"));
                }
            }
        }
        return apis;
    }

    private static void putIfMissing(Map<String, Object> values, String key, Object value) {
        if (!values.containsKey(key) && value != null) {
            values.put(key, value);
        }
    }

    private static String apiKey(Map<String, Object> values) {
        return String.valueOf(values.get("method")) + " " + String.valueOf(values.get("path"));
    }
}
