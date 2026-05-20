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
                int maskedFieldCompare = compareLong(right.getMaskedFieldCount(), left.getMaskedFieldCount());
                if (maskedFieldCompare != 0) {
                    return maskedFieldCompare;
                }
                return compareLong(right.getHitCount(), left.getHitCount());
            }
        });
        ResponseRiskSummary summary = new ResponseRiskSummary(report.getApiMetrics().size(), highRiskApiCount,
                ignoredProfiles.size(), slowApiCount);
        return new ResponseRiskAnalysis(summary, profiles, ignoredProfiles);
    }

    private ResponseRiskApiProfile profile(ApiMaskMetrics metric) {
        List<String> reasons = new ArrayList<String>();
        List<String> tags = new ArrayList<String>();
        List<String> advice = new ArrayList<String>();
        int score = 0;
        // 风险画像只基于聚合后的类型计数和接口指标，不读取原始响应内容。
        score += addTypeRisk(metric, MaskTypes.PASSWORD, "PASSWORD", 45, reasons);
        score += addTypeRisk(metric, MaskTypes.ID_CARD, "ID_CARD", 35, reasons);
        score += addTypeRisk(metric, MaskTypes.BANK_CARD, "BANK_CARD", 35, reasons);
        if (metric.getMaskedFieldCount() >= HIGH_FIELD_COUNT) {
            reasons.add("HIGH_FIELD_COUNT");
            tags.add("多脱敏字段");
            score += 20;
        }
        if (metric.getHitCount() >= HIGH_FREQUENCY) {
            reasons.add("HIGH_FREQUENCY");
            tags.add("高频接口");
            score += 15;
        }
        if (metric.isIgnored()) {
            reasons.add("IGNORED_RESPONSE");
            tags.add("明文豁免");
            advice.add("复核该接口是否仍需要明文输出。");
            score += 30;
        }
        if (!reasons.isEmpty() && advice.isEmpty()) {
            advice.add("复核响应字段和脱敏规则，确认接口只返回必要字段。");
        }
        PerformanceProfile performance = performance(metric);
        if (!performance.getWarnings().isEmpty()) {
            tags.add("慢脱敏");
            advice.add("单独复核响应对象大小和脱敏规则成本。");
        }
        addTypeTags(metric, tags);
        return new ResponseRiskApiProfile(metric.getMethod(), metric.getPath(), metric.isIgnored(),
                metric.getIgnoreReason(), metric.getHitCount(), metric.getMaskedFieldCount(), Math.min(100, score),
                level(score, metric), reasons, tags, advice, performance, metric.getMaskTypeCounts());
    }

    private int compareLong(long left, long right) {
        return left < right ? -1 : (left == right ? 0 : 1);
    }

    private int addTypeRisk(ApiMaskMetrics metric, String type, String reason, int score, List<String> reasons) {
        if (metric.getMaskTypeCounts().containsKey(type) && metric.getMaskTypeCounts().get(type) > 0) {
            reasons.add(reason);
            return score;
        }
        return 0;
    }

    private void addTypeTags(ApiMaskMetrics metric, List<String> tags) {
        for (String type : metric.getMaskTypeCounts().keySet()) {
            tags.add(typeTag(type));
        }
    }

    private String typeTag(String type) {
        if (MaskTypes.PASSWORD.equals(type)) {
            return "密码脱敏";
        }
        if (MaskTypes.ID_CARD.equals(type)) {
            return "身份证脱敏";
        }
        if (MaskTypes.BANK_CARD.equals(type)) {
            return "银行卡脱敏";
        }
        if (MaskTypes.MOBILE.equals(type)) {
            return "手机号脱敏";
        }
        if (MaskTypes.EMAIL.equals(type)) {
            return "邮箱脱敏";
        }
        if (MaskTypes.CHINESE_NAME.equals(type)) {
            return "姓名脱敏";
        }
        if (MaskTypes.ADDRESS.equals(type)) {
            return "地址脱敏";
        }
        return type + " 脱敏";
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
