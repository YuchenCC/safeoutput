package com.safeoutput.dashboard.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safeoutput.dashboard.autoconfigure.SafeOutputDashboardProperties;
import com.safeoutput.dashboard.service.SafeOutputDashboardAssembler;
import com.safeoutput.dashboard.service.SafeOutputDashboardReportFileStore;
import com.safeoutput.report.MaskMetricsCollector;
import com.safeoutput.report.MaskReport;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputConfiguredKeys;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputProperties;
import com.safeoutput.report.LogRuleSuggestionAnalyzer;
import com.safeoutput.report.LogRuleSuggestionReport;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    private final SafeOutputDashboardReportFileStore reportFileStore;

    public SafeOutputDashboardController(SafeOutputDashboardProperties properties, ObjectMapper objectMapper,
            SafeOutputDashboardAssembler dashboardAssembler, ObjectProvider<MaskMetricsCollector> metricsCollectors,
            ObjectProvider<SafeOutputProperties> safeOutputProperties,
            SafeOutputDashboardReportFileStore reportFileStore) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.dashboardAssembler = dashboardAssembler;
        this.metricsCollectors = metricsCollectors;
        this.safeOutputProperties = safeOutputProperties;
        this.reportFileStore = reportFileStore;
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

    @PostMapping("/reports/list")
    public ResponseEntity<Map<String, Object>> reportFiles() throws IOException {
        List<Map<String, Object>> files = reportFileStore.list();
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("count", files.size());
        response.put("files", files);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reports/view")
    public ResponseEntity<Map<String, Object>> reportView(@RequestBody ReportViewRequest request) throws IOException {
        Path path = reportFileStore.find(request == null ? null : request.getFilename());
        if (path == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("report_not_found"));
        }
        Map<String, Object> report = objectMapper.readValue(reportFileStore.read(path),
                new TypeReference<Map<String, Object>>() { });
        return ResponseEntity.ok(dashboardAssembler.historical(path.getFileName().toString(), report));
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

    private static Map<String, Object> error(String code) {
        Map<String, Object> error = new LinkedHashMap<String, Object>();
        error.put("error", code);
        return error;
    }

    public static final class ReportViewRequest {

        private String filename;

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }
    }
}
