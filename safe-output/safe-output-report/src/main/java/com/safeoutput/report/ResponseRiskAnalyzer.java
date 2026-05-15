package com.safeoutput.report;

import com.safeoutput.core.MaskTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ResponseRiskAnalyzer {

    private static final long HIGH_FIELD_COUNT = 5L;

    private static final long HIGH_FREQUENCY = 10L;

    public ResponseRiskAnalysis analyze(MaskReport report) {
        List<ResponseRiskApiProfile> profiles = new ArrayList<ResponseRiskApiProfile>();
        List<ResponseRiskApiProfile> ignoredProfiles = new ArrayList<ResponseRiskApiProfile>();
        long highRiskApiCount = 0;
        long slowApiCount = 0;
        for (ApiMaskMetrics metric : report.getApiMetrics()) {
            ResponseRiskApiProfile profile = profile(metric);
            profiles.add(profile);
            if (metric.isIgnored()) {
                ignoredProfiles.add(profile);
            }
            if (profile.getRiskLevel() == ApiRiskLevel.HIGH || profile.getRiskLevel() == ApiRiskLevel.CRITICAL
                    || profile.getRiskLevel() == ApiRiskLevel.IGNORED_HIGH) {
                highRiskApiCount++;
            }
            if (metric.getSlowMaskCount() > 0) {
                slowApiCount++;
            }
        }
        profiles.sort(new Comparator<ResponseRiskApiProfile>() {
            @Override
            public int compare(ResponseRiskApiProfile left, ResponseRiskApiProfile right) {
                return right.getRiskScore() - left.getRiskScore();
            }
        });
        ResponseRiskSummary summary = new ResponseRiskSummary(report.getApiMetrics().size(), highRiskApiCount,
                ignoredProfiles.size(), slowApiCount);
        return new ResponseRiskAnalysis(summary, profiles, ignoredProfiles);
    }

    private ResponseRiskApiProfile profile(ApiMaskMetrics metric) {
        List<String> reasons = new ArrayList<String>();
        List<String> advice = new ArrayList<String>();
        int score = 0;
        // 风险画像只基于聚合后的类型计数和接口指标，不读取原始响应内容。
        score += addTypeRisk(metric, MaskTypes.PASSWORD, "PASSWORD", 45, reasons);
        score += addTypeRisk(metric, MaskTypes.ID_CARD, "ID_CARD", 35, reasons);
        score += addTypeRisk(metric, MaskTypes.BANK_CARD, "BANK_CARD", 35, reasons);
        if (metric.getMaskedFieldCount() >= HIGH_FIELD_COUNT) {
            reasons.add("HIGH_FIELD_COUNT");
            score += 20;
        }
        if (metric.getHitCount() >= HIGH_FREQUENCY) {
            reasons.add("HIGH_FREQUENCY");
            score += 15;
        }
        if (metric.isIgnored()) {
            reasons.add("IGNORED_RESPONSE");
            advice.add("Review whether the ignored response still needs plaintext output.");
            score += 30;
        }
        if (!reasons.isEmpty() && advice.isEmpty()) {
            advice.add("Review response rules and confirm the interface only returns required fields.");
        }
        PerformanceProfile performance = performance(metric);
        if (!performance.getWarnings().isEmpty()) {
            advice.add("Review response object size and masking rule cost separately from sensitive risk.");
        }
        return new ResponseRiskApiProfile(metric.getMethod(), metric.getPath(), metric.isIgnored(),
                metric.getIgnoreReason(), Math.min(100, score), level(score, metric), reasons, advice, performance,
                metric.getMaskTypeCounts());
    }

    private int addTypeRisk(ApiMaskMetrics metric, String type, String reason, int score, List<String> reasons) {
        if (metric.getMaskTypeCounts().containsKey(type) && metric.getMaskTypeCounts().get(type) > 0) {
            reasons.add(reason);
            return score;
        }
        return 0;
    }

    private PerformanceProfile performance(ApiMaskMetrics metric) {
        List<String> warnings = new ArrayList<String>();
        if (metric.getSlowMaskCount() > 0) {
            warnings.add("SLOW_MASKING");
        }
        return new PerformanceProfile(metric.getAverageElapsedNanos(), metric.getMaxElapsedNanos(),
                metric.getSlowMaskCount(), warnings);
    }

    private ApiRiskLevel level(int score, ApiMaskMetrics metric) {
        if (metric.isIgnored()) {
            // Ignore 表示显式豁免脱敏，不表示风险消失；报告中单独标为高风险豁免。
            return ApiRiskLevel.IGNORED_HIGH;
        }
        if (score >= 80) {
            return ApiRiskLevel.CRITICAL;
        }
        if (score >= 50) {
            return ApiRiskLevel.HIGH;
        }
        if (score > 0) {
            return ApiRiskLevel.MEDIUM;
        }
        return ApiRiskLevel.LOW;
    }
}
