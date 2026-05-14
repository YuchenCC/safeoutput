package com.safeoutput.report;

import com.safeoutput.core.LogRuleSuggestionMetric;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LogRuleSuggestionAnalyzer {

    public LogRuleSuggestionReport analyze(List<LogRuleSuggestionMetric> metrics, List<String> configuredKeys) {
        Set<String> configured = normalize(configuredKeys);
        List<LogRuleSuggestion> suggestions = new ArrayList<LogRuleSuggestion>();
        for (LogRuleSuggestionMetric metric : metrics) {
            if (configured.contains(metric.getKey())) {
                continue;
            }
            suggestions.add(new LogRuleSuggestion(metric.getKey(), metric.getType(), metric.getHitCount(),
                    confidence(metric.getHitCount()), metric.getEvidence()));
        }
        return new LogRuleSuggestionReport(suggestions, configSnippet(suggestions));
    }

    private String configSnippet(List<LogRuleSuggestion> suggestions) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("safe-output:\n");
        yaml.append("  rules:\n");
        int index = 0;
        for (LogRuleSuggestion suggestion : suggestions) {
            if (suggestion.getConfidence() == LogRuleSuggestionConfidence.LOW) {
                continue;
            }
            // 只为中高置信度生成候选配置，且默认不自动生效。
            yaml.append("    - name: suggested-").append(suggestion.getKey()).append('\n');
            yaml.append("      keys:\n");
            yaml.append("        - ").append(suggestion.getKey()).append('\n');
            yaml.append("      type: ").append(suggestion.getSuggestedType()).append('\n');
            yaml.append("      enabled: false\n");
            index++;
        }
        return index == 0 ? "" : yaml.toString();
    }

    private LogRuleSuggestionConfidence confidence(long hitCount) {
        if (hitCount >= 5) {
            return LogRuleSuggestionConfidence.HIGH;
        }
        if (hitCount >= 2) {
            return LogRuleSuggestionConfidence.MEDIUM;
        }
        return LogRuleSuggestionConfidence.LOW;
    }

    private Set<String> normalize(List<String> keys) {
        Set<String> normalized = new HashSet<String>();
        if (keys == null) {
            return normalized;
        }
        for (String key : keys) {
            if (key != null && !key.trim().isEmpty()) {
                normalized.add(key.trim().toLowerCase(Locale.ENGLISH));
            }
        }
        return normalized;
    }
}
