package com.safeoutput.report;

public final class ResponseRiskSummary {

    private final long apiCount;

    private final long highRiskApiCount;

    private final long ignoredApiCount;

    private final long slowApiCount;

    ResponseRiskSummary(long apiCount, long highRiskApiCount, long ignoredApiCount, long slowApiCount) {
        this.apiCount = apiCount;
        this.highRiskApiCount = highRiskApiCount;
        this.ignoredApiCount = ignoredApiCount;
        this.slowApiCount = slowApiCount;
    }

    public long getApiCount() {
        return apiCount;
    }

    public long getHighRiskApiCount() {
        return highRiskApiCount;
    }

    public long getIgnoredApiCount() {
        return ignoredApiCount;
    }

    public long getSlowApiCount() {
        return slowApiCount;
    }
}
