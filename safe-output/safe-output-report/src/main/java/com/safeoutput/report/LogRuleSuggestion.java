package com.safeoutput.report;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class LogRuleSuggestion {

    private final String key;

    private final String suggestedType;

    private final long hitCount;

    private final LogRuleSuggestionConfidence confidence;

    private final String evidence;

    private final List<String> effectScopes;

    private final boolean autoApply;

    LogRuleSuggestion(String key, String suggestedType, long hitCount, LogRuleSuggestionConfidence confidence,
            String evidence) {
        this.key = key;
        this.suggestedType = suggestedType;
        this.hitCount = hitCount;
        this.confidence = confidence;
        this.evidence = evidence;
        this.effectScopes = Collections.unmodifiableList(Arrays.asList("RESPONSE", "LOG", "MANUAL_OBJECT"));
        this.autoApply = false;
    }

    public String getKey() {
        return key;
    }

    public String getSuggestedType() {
        return suggestedType;
    }

    public long getHitCount() {
        return hitCount;
    }

    public LogRuleSuggestionConfidence getConfidence() {
        return confidence;
    }

    public String getEvidence() {
        return evidence;
    }

    public List<String> getEffectScopes() {
        return effectScopes;
    }

    public boolean isAutoApply() {
        return autoApply;
    }
}
