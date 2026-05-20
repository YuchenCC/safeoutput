package com.safeoutput.dashboard.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safeoutput.dashboard.autoconfigure.SafeOutputDashboardProperties;
import com.safeoutput.dashboard.service.SafeOutputDashboardAssembler;
import com.safeoutput.report.MaskMetricsCollector;
import com.safeoutput.report.MaskReport;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputConfiguredKeys;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputProperties;
import com.safeoutput.report.LogRuleSuggestionAnalyzer;
import com.safeoutput.report.LogRuleSuggestionReport;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${safe-output.dashboard.path-prefix:/safe-output/dashboard}/api")
public class SafeOutputDashboardController {

    private final SafeOutputDashboardProperties properties;

    private final ObjectMapper objectMapper;

    private final SafeOutputDashboardAssembler dashboardAssembler;

    private final ObjectProvider<MaskMetricsCollector> metricsCollectors;

    private final ObjectProvider<SafeOutputProperties> safeOutputProperties;

    public SafeOutputDashboardController(SafeOutputDashboardProperties properties, ObjectMapper objectMapper,
            SafeOutputDashboardAssembler dashboardAssembler, ObjectProvider<MaskMetricsCollector> metricsCollectors,
            ObjectProvider<SafeOutputProperties> safeOutputProperties) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.dashboardAssembler = dashboardAssembler;
        this.metricsCollectors = metricsCollectors;
        this.safeOutputProperties = safeOutputProperties;
    }

    @PostMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("enabled", Boolean.TRUE);
        response.put("pathPrefix", properties.getPathPrefix());
        response.put("jsonMapper", objectMapper.getClass().getName());
        return response;
    }

    @PostMapping("/overview")
    public Map<String, Object> overview() {
        return dashboardAssembler.realtime(snapshot(), logSuggestionReport());
    }

    @PostMapping("/response-risk")
    public Map<String, Object> responseRisk() {
        return dashboardAssembler.responseRisk(snapshot());
    }

    @PostMapping("/log-suggestions")
    public Map<String, Object> logSuggestions() {
        LogRuleSuggestionReport report = logSuggestionReport();
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("logRuleSuggestions", report.getLogRuleSuggestions());
        response.put("configSnippet", report.getConfigSnippet());
        return response;
    }

    private MaskReport snapshot() {
        MaskMetricsCollector collector = metricsCollectors.getIfAvailable();
        if (collector == null) {
            return new MaskMetricsCollector(1).snapshot();
        }
        try {
            return collector.snapshot();
        } catch (RuntimeException ex) {
            return new MaskMetricsCollector(1).snapshot();
        }
    }

    private LogRuleSuggestionReport logSuggestionReport() {
        MaskMetricsCollector collector = metricsCollectors.getIfAvailable();
        if (collector == null) {
            return new LogRuleSuggestionAnalyzer().analyze(java.util.Collections.emptyList(),
                    java.util.Collections.emptyList());
        }
        try {
            return new LogRuleSuggestionAnalyzer().analyze(collector.snapshotSuggestions(),
                    SafeOutputConfiguredKeys.from(safeOutputProperties.getIfAvailable()));
        } catch (RuntimeException ex) {
            return new LogRuleSuggestionAnalyzer().analyze(java.util.Collections.emptyList(),
                    java.util.Collections.emptyList());
        }
    }
}
