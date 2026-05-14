package com.safeoutput.core;

public final class LogRuleSuggestionMetric {

    private final String key;

    private final String type;

    private final long hitCount;

    private final long firstSeenTimeMillis;

    private final long lastSeenTimeMillis;

    private final String evidence;

    LogRuleSuggestionMetric(String key, String type, long hitCount, long firstSeenTimeMillis, long lastSeenTimeMillis,
            String evidence) {
        this.key = key;
        this.type = type;
        this.hitCount = hitCount;
        this.firstSeenTimeMillis = firstSeenTimeMillis;
        this.lastSeenTimeMillis = lastSeenTimeMillis;
        this.evidence = evidence;
    }

    public String getKey() {
        return key;
    }

    public String getType() {
        return type;
    }

    public long getHitCount() {
        return hitCount;
    }

    public long getFirstSeenTimeMillis() {
        return firstSeenTimeMillis;
    }

    public long getLastSeenTimeMillis() {
        return lastSeenTimeMillis;
    }

    public String getEvidence() {
        return evidence;
    }
}
