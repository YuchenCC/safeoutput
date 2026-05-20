package com.safeoutput.report;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResponseRiskApiProfile {

    private final String method;

    private final String path;

    private final boolean ignored;

    private final String ignoreReason;

    private final long hitCount;

    private final long maskedFieldCount;

    private final int riskScore;

    private final ApiRiskLevel riskLevel;

    private final List<String> riskReasons;

    private final List<String> riskTags;

    private final List<String> governanceAdvice;

    private final PerformanceProfile performanceProfile;

    private final Map<String, Long> maskTypeCounts;

    ResponseRiskApiProfile(String method, String path, boolean ignored, String ignoreReason, long hitCount,
            long maskedFieldCount, int riskScore, ApiRiskLevel riskLevel, List<String> riskReasons,
            List<String> riskTags, List<String> governanceAdvice, PerformanceProfile performanceProfile,
            Map<String, Long> maskTypeCounts) {
        this.method = method;
        this.path = path;
        this.ignored = ignored;
        this.ignoreReason = ignoreReason;
        this.hitCount = hitCount;
        this.maskedFieldCount = maskedFieldCount;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.riskReasons = Collections.unmodifiableList(riskReasons);
        this.riskTags = Collections.unmodifiableList(riskTags);
        this.governanceAdvice = Collections.unmodifiableList(governanceAdvice);
        this.performanceProfile = performanceProfile;
        this.maskTypeCounts = Collections.unmodifiableMap(new LinkedHashMap<String, Long>(maskTypeCounts));
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public String getIgnoreReason() {
        return ignoreReason;
    }

    public long getHitCount() {
        return hitCount;
    }

    public long getMaskedFieldCount() {
        return maskedFieldCount;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public ApiRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public List<String> getRiskReasons() {
        return riskReasons;
    }

    public List<String> getRiskTags() {
        return riskTags;
    }

    public List<String> getGovernanceAdvice() {
        return governanceAdvice;
    }

    public PerformanceProfile getPerformanceProfile() {
        return performanceProfile;
    }

    public Map<String, Long> getMaskTypeCounts() {
        return maskTypeCounts;
    }
}
