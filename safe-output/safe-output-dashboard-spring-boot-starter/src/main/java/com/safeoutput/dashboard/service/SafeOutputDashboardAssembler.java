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

    private static Map<String, Long> sceneTrend(MaskReport report) {
        Map<String, Long> sceneTrend = new LinkedHashMap<String, Long>();
        sceneTrend.put("response", report.getResponseCount());
        sceneTrend.put("log", report.getLogCount());
        sceneTrend.put("manual", report.getManualCount());
        return sceneTrend;
    }
}
