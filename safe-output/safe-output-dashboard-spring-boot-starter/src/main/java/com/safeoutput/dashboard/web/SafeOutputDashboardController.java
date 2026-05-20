package com.safeoutput.dashboard.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safeoutput.dashboard.autoconfigure.SafeOutputDashboardProperties;
import com.safeoutput.dashboard.service.SafeOutputDashboardAssembler;
import com.safeoutput.report.MaskMetricsCollector;
import com.safeoutput.report.MaskReport;

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

    public SafeOutputDashboardController(SafeOutputDashboardProperties properties, ObjectMapper objectMapper,
            SafeOutputDashboardAssembler dashboardAssembler, ObjectProvider<MaskMetricsCollector> metricsCollectors) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.dashboardAssembler = dashboardAssembler;
        this.metricsCollectors = metricsCollectors;
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
        return dashboardAssembler.realtime(snapshot(), null);
    }

    @PostMapping("/response-risk")
    public Map<String, Object> responseRisk() {
        return dashboardAssembler.responseRisk(snapshot());
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
}
