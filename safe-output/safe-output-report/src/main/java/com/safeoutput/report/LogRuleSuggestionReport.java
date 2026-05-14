package com.safeoutput.report;

import java.util.Collections;
import java.util.List;

public final class LogRuleSuggestionReport {

    private final List<LogRuleSuggestion> logRuleSuggestions;

    private final String configSnippet;

    LogRuleSuggestionReport(List<LogRuleSuggestion> logRuleSuggestions, String configSnippet) {
        this.logRuleSuggestions = Collections.unmodifiableList(logRuleSuggestions);
        this.configSnippet = configSnippet;
    }

    public List<LogRuleSuggestion> getLogRuleSuggestions() {
        return logRuleSuggestions;
    }

    public String getConfigSnippet() {
        return configSnippet;
    }
}
