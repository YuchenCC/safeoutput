package com.safeoutput.report;

import java.util.List;
import java.util.Map;

final class MaskReportJsonWriter {

    String write(MaskReport report, LogRuleSuggestionReport suggestions) {
        // 手写 JSON 只序列化报告模型中的聚合字段，避免引入额外依赖或误写敏感样本。
        StringBuilder json = new StringBuilder();
        json.append('{');
        field(json, "totalCount", report.getTotalCount()).append(',');
        field(json, "responseCount", report.getResponseCount()).append(',');
        field(json, "logCount", report.getLogCount()).append(',');
        field(json, "manualCount", report.getManualCount()).append(',');
        field(json, "failureCount", report.getFailureCount()).append(',');
        field(json, "averageElapsedNanos", report.getAverageElapsedNanos()).append(',');
        field(json, "maxElapsedNanos", report.getMaxElapsedNanos()).append(',');
        json.append("\"maskTypeCounts\":");
        maskTypeCounts(json, report.getMaskTypeCounts()).append(',');
        json.append("\"unknownTypeCounts\":");
        maskTypeCounts(json, report.getUnknownTypeCounts()).append(',');
        json.append("\"apiMetrics\":");
        apiMetrics(json, report.getApiMetrics()).append(',');
        ResponseRiskAnalysis analysis = report.getResponseRiskAnalysis();
        json.append("\"responseRiskSummary\":");
        responseRiskSummary(json, analysis.getResponseRiskSummary()).append(',');
        json.append("\"topRiskApis\":");
        riskApis(json, analysis.getTopRiskApis()).append(',');
        json.append("\"ignoredRiskApis\":");
        riskApis(json, analysis.getIgnoredRiskApis()).append(',');
        json.append("\"logRuleSuggestions\":");
        logRuleSuggestions(json, suggestions.getLogRuleSuggestions()).append(',');
        stringField(json, "configSnippet", suggestions.getConfigSnippet());
        json.append('}');
        return json.toString();
    }

    private static StringBuilder logRuleSuggestions(StringBuilder json, List<LogRuleSuggestion> suggestions) {
        json.append('[');
        for (int i = 0; i < suggestions.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            LogRuleSuggestion suggestion = suggestions.get(i);
            json.append('{');
            stringField(json, "key", suggestion.getKey()).append(',');
            stringField(json, "suggestedType", suggestion.getSuggestedType()).append(',');
            field(json, "hitCount", suggestion.getHitCount()).append(',');
            stringField(json, "confidence", suggestion.getConfidence().name()).append(',');
            stringField(json, "evidence", suggestion.getEvidence()).append(',');
            json.append("\"effectScopes\":");
            strings(json, suggestion.getEffectScopes()).append(',');
            booleanField(json, "autoApply", suggestion.isAutoApply());
            json.append('}');
        }
        json.append(']');
        return json;
    }

    private static StringBuilder responseRiskSummary(StringBuilder json, ResponseRiskSummary summary) {
        json.append('{');
        field(json, "apiCount", summary.getApiCount()).append(',');
        field(json, "highRiskApiCount", summary.getHighRiskApiCount()).append(',');
        field(json, "ignoredApiCount", summary.getIgnoredApiCount()).append(',');
        field(json, "slowApiCount", summary.getSlowApiCount());
        json.append('}');
        return json;
    }

    private static StringBuilder riskApis(StringBuilder json, List<ResponseRiskApiProfile> apis) {
        json.append('[');
        for (int i = 0; i < apis.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            riskApi(json, apis.get(i));
        }
        json.append(']');
        return json;
    }

    private static StringBuilder riskApi(StringBuilder json, ResponseRiskApiProfile api) {
        json.append('{');
        stringField(json, "method", api.getMethod()).append(',');
        stringField(json, "path", api.getPath()).append(',');
        booleanField(json, "ignored", api.isIgnored()).append(',');
        stringField(json, "ignoreReason", api.getIgnoreReason()).append(',');
        field(json, "riskScore", api.getRiskScore()).append(',');
        stringField(json, "riskLevel", api.getRiskLevel().name()).append(',');
        json.append("\"riskReasons\":");
        strings(json, api.getRiskReasons()).append(',');
        json.append("\"governanceAdvice\":");
        strings(json, api.getGovernanceAdvice()).append(',');
        json.append("\"performanceProfile\":");
        performanceProfile(json, api.getPerformanceProfile()).append(',');
        json.append("\"maskTypeCounts\":");
        maskTypeCounts(json, api.getMaskTypeCounts());
        json.append('}');
        return json;
    }

    private static StringBuilder performanceProfile(StringBuilder json, PerformanceProfile profile) {
        json.append('{');
        field(json, "averageElapsedNanos", profile.getAverageElapsedNanos()).append(',');
        field(json, "maxElapsedNanos", profile.getMaxElapsedNanos()).append(',');
        field(json, "slowMaskCount", profile.getSlowMaskCount()).append(',');
        json.append("\"warnings\":");
        strings(json, profile.getWarnings());
        json.append('}');
        return json;
    }

    private static StringBuilder apiMetrics(StringBuilder json, List<ApiMaskMetrics> metrics) {
        json.append('[');
        for (int i = 0; i < metrics.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            ApiMaskMetrics metric = metrics.get(i);
            json.append('{');
            stringField(json, "method", metric.getMethod()).append(',');
            stringField(json, "path", metric.getPath()).append(',');
            field(json, "hitCount", metric.getHitCount()).append(',');
            booleanField(json, "ignored", metric.isIgnored()).append(',');
            stringField(json, "ignoreReason", metric.getIgnoreReason()).append(',');
            field(json, "failureCount", metric.getFailureCount()).append(',');
            field(json, "maskedFieldCount", metric.getMaskedFieldCount()).append(',');
            field(json, "averageElapsedNanos", metric.getAverageElapsedNanos()).append(',');
            field(json, "maxElapsedNanos", metric.getMaxElapsedNanos()).append(',');
            field(json, "slowMaskCount", metric.getSlowMaskCount()).append(',');
            stringField(json, "riskLevel", metric.getRiskLevel().name()).append(',');
            json.append("\"maskTypeCounts\":");
            maskTypeCounts(json, metric.getMaskTypeCounts());
            json.append('}');
        }
        json.append(']');
        return json;
    }

    private static StringBuilder maskTypeCounts(StringBuilder json, Map<String, Long> counts) {
        json.append('{');
        int index = 0;
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            if (index > 0) {
                json.append(',');
            }
            string(json, entry.getKey()).append(':').append(entry.getValue());
            index++;
        }
        json.append('}');
        return json;
    }

    private static StringBuilder strings(StringBuilder json, List<String> values) {
        json.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            string(json, values.get(i));
        }
        json.append(']');
        return json;
    }

    private static StringBuilder field(StringBuilder json, String name, long value) {
        return string(json, name).append(':').append(value);
    }

    private static StringBuilder booleanField(StringBuilder json, String name, boolean value) {
        return string(json, name).append(':').append(value);
    }

    private static StringBuilder stringField(StringBuilder json, String name, String value) {
        string(json, name).append(':');
        if (value == null) {
            return json.append("null");
        }
        return string(json, value);
    }

    private static StringBuilder string(StringBuilder json, String value) {
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '"' || ch == '\\') {
                json.append('\\').append(ch);
            } else if (ch == '\n') {
                json.append("\\n");
            } else if (ch == '\r') {
                json.append("\\r");
            } else if (ch == '\t') {
                json.append("\\t");
            } else {
                json.append(ch);
            }
        }
        return json.append('"');
    }
}
