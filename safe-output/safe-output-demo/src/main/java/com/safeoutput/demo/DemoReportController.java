package com.safeoutput.demo;

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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public DemoReportController(MaskReportExporter exporter, MaskMetricsCollector metricsCollector,
            SafeOutputProperties properties, ObjectMapper objectMapper) {
        this.exporter = exporter;
        this.metricsCollector = metricsCollector;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/demo/report/snapshot")
    public MaskReport snapshot() {
        return metricsCollector.snapshot();
    }

    @GetMapping("/demo/report/dashboard")
    public Map<String, Object> dashboard() {
        MaskReport report = metricsCollector.snapshot();
        ResponseRiskAnalysis riskAnalysis = report.getResponseRiskAnalysis();
        LogRuleSuggestionReport suggestionReport = new LogRuleSuggestionAnalyzer().analyze(
                metricsCollector.snapshotSuggestions(), SafeOutputConfiguredKeys.from(properties));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("totalCount", report.getTotalCount());
        result.put("responseCount", report.getResponseCount());
        result.put("logCount", report.getLogCount());
        result.put("manualCount", report.getManualCount());
        result.put("failureCount", report.getFailureCount());
        result.put("highRiskApiCount", riskAnalysis.getResponseRiskSummary().getHighRiskApiCount());
        result.put("apiCount", riskAnalysis.getResponseRiskSummary().getApiCount());
        result.put("ignoredApiCount", riskAnalysis.getResponseRiskSummary().getIgnoredApiCount());
        result.put("slowApiCount", riskAnalysis.getResponseRiskSummary().getSlowApiCount());
        result.put("suggestionCount", suggestionReport.getLogRuleSuggestions().size());
        result.put("averageElapsedNanos", report.getAverageElapsedNanos());
        result.put("maxElapsedNanos", report.getMaxElapsedNanos());
        result.put("maskTypeCounts", report.getMaskTypeCounts());
        result.put("topRiskApis", riskAnalysis.getTopRiskApis());
        result.put("ignoredRiskApis", riskAnalysis.getIgnoredRiskApis());
        result.put("logRuleSuggestions", suggestionReport.getLogRuleSuggestions());

        Map<String, Long> sceneTrend = new LinkedHashMap<String, Long>();
        sceneTrend.put("response", report.getResponseCount());
        sceneTrend.put("log", report.getLogCount());
        sceneTrend.put("manual", report.getManualCount());
        result.put("sceneTrend", sceneTrend);

        return result;
    }

    @GetMapping("/demo/report/export")
    public Map<String, String> export() {
        Path path = exporter.exportNow();
        return Collections.singletonMap("path", path == null ? "" : path.toString());
    }

    @GetMapping("/demo/report/files")
    public Map<String, Object> files() throws IOException {
        List<Map<String, Object>> files = reportFiles();
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("count", files.size());
        response.put("files", files);
        return response;
    }

    @GetMapping(value = "/demo/report/files/{name:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> file(@PathVariable("name") String name) throws IOException {
        Path path = safeReportPath(name);
        if (path == null || !Files.isRegularFile(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\":\"report_not_found\"}");
        }
        return ResponseEntity.ok(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    @GetMapping("/demo/report/files/{name:.+}/dashboard")
    public ResponseEntity<Map<String, Object>> fileDashboard(@PathVariable("name") String name) throws IOException {
        Path path = safeReportPath(name);
        if (path == null || !Files.isRegularFile(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("report_not_found"));
        }
        Map<String, Object> report = objectMapper.readValue(Files.readAllBytes(path),
                new TypeReference<Map<String, Object>>() { });
        Map<String, Object> dashboard = new LinkedHashMap<String, Object>();
        dashboard.put("filename", path.getFileName().toString());
        dashboard.put("totalCount", report.get("totalCount"));
        dashboard.put("responseCount", report.get("responseCount"));
        dashboard.put("logCount", report.get("logCount"));
        dashboard.put("manualCount", report.get("manualCount"));
        dashboard.put("failureCount", report.get("failureCount"));
        dashboard.put("averageElapsedNanos", report.get("averageElapsedNanos"));
        dashboard.put("maxElapsedNanos", report.get("maxElapsedNanos"));
        dashboard.put("maskTypeCounts", report.get("maskTypeCounts"));
        addRiskSummary(dashboard, report.get("responseRiskSummary"));
        dashboard.put("topRiskApis", report.get("topRiskApis"));
        dashboard.put("ignoredRiskApis", report.get("ignoredRiskApis"));
        dashboard.put("logRuleSuggestions", report.get("logRuleSuggestions"));
        dashboard.put("configSnippet", report.get("configSnippet"));
        return ResponseEntity.ok(dashboard);
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

    private List<Map<String, Object>> reportFiles() throws IOException {
        Path directory = reportDirectory();
        if (!Files.isDirectory(directory)) {
            return new ArrayList<Map<String, Object>>();
        }
        try (Stream<Path> stream = Files.list(directory)) {
            List<Path> paths = stream
                    .filter(path -> path.getFileName().toString().startsWith(properties.getReport().getFilePrefix()
                            + "-"))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
            List<Map<String, Object>> files = new ArrayList<Map<String, Object>>();
            for (Path path : paths) {
                Map<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("name", path.getFileName().toString());
                item.put("size", Files.size(path));
                item.put("modifiedAt", Files.getLastModifiedTime(path).toMillis());
                item.put("viewEndpoint", "/demo/report/files/" + path.getFileName().toString() + "/dashboard");
                files.add(item);
            }
            return files;
        }
    }

    private Path safeReportPath(String name) throws IOException {
        if (name == null || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0
                || !name.startsWith(properties.getReport().getFilePrefix() + "-") || !name.endsWith(".json")) {
            return null;
        }
        Path directory = reportDirectory();
        Path resolved = directory.resolve(name).normalize();
        if (!resolved.startsWith(directory)) {
            return null;
        }
        return resolved;
    }

    private Path reportDirectory() throws IOException {
        return Paths.get(properties.getReport().getDirectory()).toAbsolutePath().normalize();
    }

    @SuppressWarnings("unchecked")
    private static void addRiskSummary(Map<String, Object> dashboard, Object summary) {
        if (!(summary instanceof Map)) {
            return;
        }
        Map<String, Object> values = (Map<String, Object>) summary;
        dashboard.put("apiCount", values.get("apiCount"));
        dashboard.put("highRiskApiCount", values.get("highRiskApiCount"));
        dashboard.put("ignoredApiCount", values.get("ignoredApiCount"));
        dashboard.put("slowApiCount", values.get("slowApiCount"));
    }

    private static Map<String, Object> error(String code) {
        Map<String, Object> error = new LinkedHashMap<String, Object>();
        error.put("error", code);
        return error;
    }
}
