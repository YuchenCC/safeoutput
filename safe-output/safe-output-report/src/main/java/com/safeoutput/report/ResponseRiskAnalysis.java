package com.safeoutput.report;

import java.util.Collections;
import java.util.List;

public final class ResponseRiskAnalysis {

    private final ResponseRiskSummary responseRiskSummary;

    private final List<ResponseRiskApiProfile> topRiskApis;

    private final List<ResponseRiskApiProfile> ignoredRiskApis;

    ResponseRiskAnalysis(ResponseRiskSummary responseRiskSummary, List<ResponseRiskApiProfile> topRiskApis,
            List<ResponseRiskApiProfile> ignoredRiskApis) {
        this.responseRiskSummary = responseRiskSummary;
        this.topRiskApis = Collections.unmodifiableList(topRiskApis);
        this.ignoredRiskApis = Collections.unmodifiableList(ignoredRiskApis);
    }

    public ResponseRiskSummary getResponseRiskSummary() {
        return responseRiskSummary;
    }

    public List<ResponseRiskApiProfile> getTopRiskApis() {
        return topRiskApis;
    }

    public List<ResponseRiskApiProfile> getIgnoredRiskApis() {
        return ignoredRiskApis;
    }
}
