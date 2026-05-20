package com.safeoutput.demo.report;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safeoutput.report.LogRuleSuggestionAnalyzer;
import com.safeoutput.report.LogRuleSuggestionReport;
import com.safeoutput.report.MaskMetricsCollector;
import com.safeoutput.report.MaskReport;
import com.safeoutput.report.MaskReportExporter;
import com.safeoutput.report.ResponseRiskAnalysis;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputConfiguredKeys;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputProperties;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoReportController {

    private final MaskReportExporter exporter;

    private final MaskMetricsCollector metricsCollector;

    private final SafeOutputProperties properties;

    private final ObjectMapper objectMapper;

    private final DemoReportFileStore fileStore;

    private final DemoReportDashboardAssembler dashboardAssembler;

    public DemoReportController(MaskReportExporter exporter, MaskMetricsCollector metricsCollector,
            SafeOutputProperties properties, ObjectMapper objectMapper, DemoReportFileStore fileStore,
            DemoReportDashboardAssembler dashboardAssembler) {
        this.exporter = exporter;
        this.metricsCollector = metricsCollector;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.fileStore = fileStore;
        this.dashboardAssembler = dashboardAssembler;
    }

    @GetMapping("/demo/report/snapshot")
    public MaskReport snapshot() {
        return metricsCollector.snapshot();
    }

    @GetMapping("/demo/report/dashboard")
    public Map<String, Object> dashboard() {
        MaskReport report = metricsCollector.snapshot();
        LogRuleSuggestionReport suggestionReport = new LogRuleSuggestionAnalyzer().analyze(
                metricsCollector.snapshotSuggestions(), SafeOutputConfiguredKeys.from(properties));
        return dashboardAssembler.realtime(report, suggestionReport);
    }

    @GetMapping("/demo/report/export")
    public Map<String, String> export() {
        Path path = exporter.exportNow();
        return Collections.singletonMap("path", path == null ? "" : path.toString());
    }

    @GetMapping("/demo/report/files")
    public Map<String, Object> files() throws IOException {
        List<Map<String, Object>> files = fileStore.list();
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("count", files.size());
        response.put("files", files);
        return response;
    }

    @GetMapping(value = "/demo/report/files/{name:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> file(@PathVariable("name") String name) throws IOException {
        Path path = fileStore.find(name);
        if (path == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\":\"report_not_found\"}");
        }
        return ResponseEntity.ok(fileStore.read(path));
    }

    @GetMapping("/demo/report/files/{name:.+}/dashboard")
    public ResponseEntity<Map<String, Object>> fileDashboard(@PathVariable("name") String name) throws IOException {
        Path path = fileStore.find(name);
        if (path == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("report_not_found"));
        }
        Map<String, Object> report = objectMapper.readValue(fileStore.read(path),
                new TypeReference<Map<String, Object>>() { });
        return ResponseEntity.ok(dashboardAssembler.historical(path.getFileName().toString(), report));
    }

    @GetMapping("/demo/report/response-risk")
    public Map<String, Object> responseRisk() {
        ResponseRiskAnalysis analysis = metricsCollector.snapshot().getResponseRiskAnalysis();
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("responseRiskSummary", analysis.getResponseRiskSummary());
        response.put("topRiskApis", analysis.getTopRiskApis());
        response.put("ignoredRiskApis", analysis.getIgnoredRiskApis());
        return response;
    }

    @GetMapping("/demo/report/log-suggestions")
    public Map<String, Object> logSuggestions() {
        LogRuleSuggestionReport report = new LogRuleSuggestionAnalyzer().analyze(
                metricsCollector.snapshotSuggestions(), SafeOutputConfiguredKeys.from(properties));
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("logRuleSuggestions", report.getLogRuleSuggestions());
        response.put("configSnippet", report.getConfigSnippet());
        return response;
    }

    private static Map<String, Object> error(String code) {
        Map<String, Object> error = new LinkedHashMap<String, Object>();
        error.put("error", code);
        return error;
    }
}
