package com.safeoutput.core;

import java.util.List;

public interface LogRuleSuggestionCollector {

    void record(LogRuleSuggestionEvent event);

    List<LogRuleSuggestionMetric> snapshot();
}
