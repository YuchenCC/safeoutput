package com.safeoutput.dashboard.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safeoutput.core.SafeOutputMaskService;
import com.safeoutput.dashboard.autoconfigure.SafeOutputDashboardProperties;
import com.safeoutput.dashboard.service.SafeOutputDashboardAssembler;
import com.safeoutput.dashboard.service.SafeOutputDashboardReportFileStore;
import com.safeoutput.report.MaskMetricsCollector;
import com.safeoutput.report.MaskReport;
import com.safeoutput.report.MaskReportExporter;
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
import java.util.ArrayList;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${safe-output.dashboard.path-prefix:/safe-output/dashboard}/api")
public class SafeOutputDashboardController {

    private static final long MAX_UPLOAD_BYTES = 1024L * 1024L;

    private final SafeOutputDashboardProperties properties;

    private final ObjectMapper objectMapper;

    private final SafeOutputDashboardAssembler dashboardAssembler;

    private final ObjectProvider<MaskMetricsCollector> metricsCollectors;

    private final ObjectProvider<SafeOutputProperties> safeOutputProperties;

    private final SafeOutputDashboardReportFileStore reportFileStore;

    private final ObjectProvider<SafeOutputMaskService> maskServices;

    private final ObjectProvider<MaskReportExporter> reportExporters;

    public SafeOutputDashboardController(SafeOutputDashboardProperties properties, ObjectMapper objectMapper,
            SafeOutputDashboardAssembler dashboardAssembler, ObjectProvider<MaskMetricsCollector> metricsCollectors,
            ObjectProvider<SafeOutputProperties> safeOutputProperties,
            SafeOutputDashboardReportFileStore reportFileStore,
            ObjectProvider<SafeOutputMaskService> maskServices,
            ObjectProvider<MaskReportExporter> reportExporters) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.dashboardAssembler = dashboardAssembler;
        this.metricsCollectors = metricsCollectors;
        this.safeOutputProperties = safeOutputProperties;
        this.reportFileStore = reportFileStore;
        this.maskServices = maskServices;
        this.reportExporters = reportExporters;
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

    @PostMapping("/reports/export")
    public ResponseEntity<Map<String, String>> reportExport() {
        MaskReportExporter exporter = reportExporters.getIfAvailable();
        Map<String, String> response = new LinkedHashMap<String, String>();
        if (exporter == null) {
            response.put("path", "");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        Path path = exporter.exportNow();
        response.put("path", path == null ? "" : path.toString());
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

    @PostMapping(value = "/reports/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> reportUpload(@RequestParam(value = "file", required = false)
            MultipartFile file)
            throws IOException {
        if (file == null || file.isEmpty() || !isJsonFilename(file.getOriginalFilename())) {
            return ResponseEntity.badRequest().body(error("invalid_report_file"));
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error("report_file_too_large"));
        }
        Map<String, Object> report = objectMapper.readValue(file.getBytes(),
                new TypeReference<Map<String, Object>>() { });
        if (!isReportShape(report)) {
            return ResponseEntity.badRequest().body(error("invalid_report_shape"));
        }
        return ResponseEntity.ok(dashboardAssembler.historical(file.getOriginalFilename(), report));
    }

    @PostMapping("/lab/by-type")
    public ResponseEntity<List<Map<String, Object>>> maskByType(@RequestBody ByTypeRequest request) {
        SafeOutputMaskService maskService = maskServices.getIfAvailable();
        if (maskService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ArrayList<Map<String, Object>>());
        }
        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        String current = request == null ? "" : request.getValue();
        Object previous = null;
        for (int round = 1; round <= 2; round++) {
            long startedAt = System.nanoTime();
            current = maskService.mask(current, request == null ? null : request.getType());
            appendRound(response, round, current, System.nanoTime() - startedAt, previous);
            previous = current;
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/lab/object")
    public ResponseEntity<List<Map<String, Object>>> maskObject(@RequestBody(required = false) ObjectRequest request) {
        SafeOutputMaskService maskService = maskServices.getIfAvailable();
        if (maskService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ArrayList<Map<String, Object>>());
        }
        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        ManualOrder current = manualOrder(request);
        Object previous = null;
        for (int round = 1; round <= 2; round++) {
            long startedAt = System.nanoTime();
            current = (ManualOrder) maskService.maskObject(current);
            ManualOrder snapshot = snapshot(current);
            appendRound(response, round, snapshot, System.nanoTime() - startedAt, previous);
            previous = snapshot;
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/lab/strong")
    public ResponseEntity<List<Map<String, Object>>> maskStrong(@RequestBody StrongRequest request) {
        SafeOutputMaskService maskService = maskServices.getIfAvailable();
        if (maskService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ArrayList<Map<String, Object>>());
        }
        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        String current = request == null ? "" : request.getText();
        Object previous = null;
        for (int round = 1; round <= 2; round++) {
            long startedAt = System.nanoTime();
            current = maskService.maskStrong(current);
            appendRound(response, round, current, System.nanoTime() - startedAt, previous);
            previous = current;
        }
        return ResponseEntity.ok(response);
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

    private static boolean isJsonFilename(String filename) {
        return filename != null && filename.toLowerCase(java.util.Locale.ENGLISH).endsWith(".json");
    }

    private static boolean isReportShape(Map<String, Object> report) {
        return report != null
                && report.containsKey("totalCount")
                && report.containsKey("responseCount")
                && report.containsKey("logCount")
                && report.containsKey("manualCount")
                && report.containsKey("failureCount")
                && report.containsKey("maskTypeCounts")
                && (report.containsKey("responseRiskSummary") || isDashboardReportShape(report));
    }

    private static boolean isDashboardReportShape(Map<String, Object> report) {
        return report.containsKey("apiCount")
                && report.containsKey("highRiskApiCount")
                && report.containsKey("ignoredApiCount")
                && report.containsKey("slowApiCount")
                && report.containsKey("topRiskApis")
                && report.containsKey("ignoredRiskApis");
    }

    private static void appendRound(List<Map<String, Object>> response, int round, Object result, long elapsedNanos,
            Object previous) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("round", round);
        item.put("result", result);
        item.put("elapsedNanos", elapsedNanos);
        item.put("sameAsPrevious", previous != null && previous.equals(result));
        response.add(item);
    }

    private static ManualOrder manualOrder(ObjectRequest request) {
        if (request == null) {
            return new ManualOrder("张三", "13800138000", "演示商品");
        }
        return new ManualOrder(valueOrDefault(request.getRealName(), "张三"),
                valueOrDefault(request.getMobile(), "13800138000"),
                valueOrDefault(request.getName(), "演示商品"));
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private static ManualOrder snapshot(ManualOrder order) {
        return new ManualOrder(order.getRealName(), order.getMobile(), order.getName());
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

    public static final class ByTypeRequest {

        private String value;

        private String type;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static final class ObjectRequest {

        private String realName;

        private String mobile;

        private String name;

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static final class StrongRequest {

        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static final class ManualOrder {

        private String realName;

        private String mobile;

        private String name;

        ManualOrder(String realName, String mobile, String name) {
            this.realName = realName;
            this.mobile = mobile;
            this.name = name;
        }

        public String getRealName() {
            return realName;
        }

        public String getMobile() {
            return mobile;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ManualOrder)) {
                return false;
            }
            ManualOrder that = (ManualOrder) other;
            return Objects.equals(realName, that.realName)
                    && Objects.equals(mobile, that.mobile)
                    && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(realName, mobile, name);
        }
    }
}
