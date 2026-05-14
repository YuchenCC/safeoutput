package com.safeoutput.core;

public final class LogRuleSuggestionEvent {

    private final String key;

    private final String type;

    private final String evidence;

    private final long seenTimeMillis;

    public LogRuleSuggestionEvent(String key, String type, String evidence, long seenTimeMillis) {
        this.key = key;
        this.type = MaskTypes.normalize(type);
        this.evidence = evidence;
        this.seenTimeMillis = seenTimeMillis;
    }

    public String getKey() {
        return key;
    }

    public String getType() {
        return type;
    }

    public String getEvidence() {
        return evidence;
    }

    public long getSeenTimeMillis() {
        return seenTimeMillis;
    }
}
