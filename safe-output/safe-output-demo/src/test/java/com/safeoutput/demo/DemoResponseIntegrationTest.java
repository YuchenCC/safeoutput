package com.safeoutput.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.safeoutput.report.ApiMaskMetrics;
import com.safeoutput.report.MaskMetricsCollector;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(classes = DemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoResponseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MaskMetricsCollector metricsCollector;

    @Test
    void demoEndpointsMaskCommonResponseShapesWithoutControllerCalls() {
        String bean = restTemplate.getForObject("/demo/bean", String.class);
        String map = restTemplate.getForObject("/demo/map", String.class);
        String list = restTemplate.getForObject("/demo/list", String.class);
        String nested = restTemplate.getForObject("/demo/nested", String.class);

        assertTrue(bean.contains("138****8000"));
        assertTrue(bean.contains("\"name\":\"张*\""));
        assertTrue(bean.contains("\"plainNote\":\"demo note 13800138000\""));
        assertFalse(bean.contains("\"mobile\":\"13800138000\""));

        assertTrue(map.contains("foo****@example.com"));
        assertFalse(map.contains("foo@example.com"));
        assertTrue(list.contains("139****8001"));
        assertTrue(nested.contains("622202*********0123"));
    }

    @Test
    void apiIgnoreKeepsPlaintextResponseAndRecordsRiskMetric() {
        String ignored = restTemplate.getForObject("/demo/ignored", String.class);

        assertTrue(ignored.contains("\"mobile\":\"13800138000\""));
        ApiMaskMetrics metric = metricsCollector.snapshot().getApiMetric("GET", "/demo/ignored");
        assertNotNull(metric);
        assertTrue(metric.isIgnored());
    }

    @Test
    void reportSnapshotEndpointReturnsAggregatedJson() {
        restTemplate.getForObject("/demo/bean", String.class);
        String snapshot = restTemplate.getForObject("/demo/report/snapshot", String.class);

        assertTrue(snapshot.contains("\"totalCount\""));
        assertTrue(snapshot.contains("\"apiMetrics\""));
        assertTrue(snapshot.contains("/demo/bean"));
        assertFalse(snapshot.contains("13800138000"));
    }

    @Test
    void demoLog4j2AndReportScenarioExportsSanitizedSnapshot() throws Exception {
        String log4j2Xml = new String(Files.readAllBytes(Paths.get("src/main/resources/log4j2.xml")),
                StandardCharsets.UTF_8);
        assertTrue(log4j2Xml.contains("%safeOutputMsg"));

        String logs = restTemplate.getForObject("/demo/logs", String.class);
        String ignored = restTemplate.getForObject("/demo/ignored", String.class);
        String exported = restTemplate.getForObject("/demo/report/export", String.class);

        assertTrue(logs.contains("logged"));
        assertTrue(ignored.contains("13800138000"));
        assertTrue(exported.contains("demo-report"));

        String json = new String(Files.readAllBytes(latestReport()), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"ignored\":true"));
        assertTrue(json.contains("/demo/ignored"));
        assertFalse(json.contains("13800138000"));
        assertFalse(json.contains("foo@example.com"));
        assertFalse(json.contains("11010519491231002X"));
        assertFalse(json.contains("6222021234567890123"));
    }

    @Test
    void manualMaskDemoEndpointsReturnFirstSecondAndIdempotentResults() {
        Map<String, String> byTypeRequest = new LinkedHashMap<String, String>();
        byTypeRequest.put("value", "13800138000");
        byTypeRequest.put("type", "mobileM");
        String byType = restTemplate.postForObject("/demo/mask/by-type", byTypeRequest, String.class);

        String object = restTemplate.postForObject("/demo/mask/object", new LinkedHashMap<String, String>(),
                String.class);

        Map<String, String> strongRequest = new LinkedHashMap<String, String>();
        strongRequest.put("text", "联系 13800138000 foo@example.com");
        String strong = restTemplate.postForObject("/demo/mask/strong", strongRequest, String.class);

        assertTrue(byType.contains("m-138****8000"));
        assertTrue(byType.contains("\"idempotent\":true"));
        assertTrue(object.contains("\"realName\":\"张*\""));
        assertTrue(object.contains("\"mobile\":\"138****8000\""));
        assertTrue(object.contains("\"name\":\"演示商品\""));
        assertTrue(object.contains("\"idempotent\":true"));
        assertTrue(strong.contains("138****8000"));
        assertTrue(strong.contains("foo****@example.com"));
        assertTrue(strong.contains("\"idempotent\":true"));

        String snapshot = restTemplate.getForObject("/demo/report/snapshot", String.class);
        assertTrue(snapshot.contains("\"manualCount\""));
        assertFalse(snapshot.contains("13800138000"));
    }

    @Test
    void r2ReportDemoEndpointsReturnRiskAndLogSuggestionsWithoutSensitiveSamples() {
        restTemplate.getForObject("/demo/nested", String.class);
        restTemplate.getForObject("/demo/ignored", String.class);

        String responseRisk = restTemplate.getForObject("/demo/report/response-risk", String.class);
        String logSuggestions = restTemplate.getForObject("/demo/report/log-suggestions", String.class);

        assertTrue(responseRisk.contains("\"responseRiskSummary\""));
        assertTrue(responseRisk.contains("\"topRiskApis\""));
        assertTrue(responseRisk.contains("\"riskReasons\""));
        assertTrue(responseRisk.contains("\"governanceAdvice\""));
        assertTrue(responseRisk.contains("\"performanceProfile\""));
        assertTrue(responseRisk.contains("\"ignoredRiskApis\""));
        assertTrue(logSuggestions.contains("\"logRuleSuggestions\""));
        assertTrue(logSuggestions.contains("\"suggestedType\""));
        assertTrue(logSuggestions.contains("\"confidence\""));
        assertTrue(logSuggestions.contains("\"evidence\""));
        assertTrue(logSuggestions.contains("\"effectScopes\""));
        assertTrue(logSuggestions.contains("\"configSnippet\""));
        assertTrue(logSuggestions.contains("phoneno"));
        assertTrue(logSuggestions.contains("certnum"));
        assertTrue(logSuggestions.contains("mailaddr"));
        assertFalse(responseRisk.contains("13800138000"));
        assertFalse(responseRisk.contains("6222021234567890123"));
        assertFalse(logSuggestions.contains("13800138000"));
        assertFalse(logSuggestions.contains("11010519491231002X"));
        assertFalse(logSuggestions.contains("foo@example.com"));
    }

    private static Path latestReport() throws Exception {
        try (Stream<Path> stream = Files.list(Paths.get("target/safe-output-demo-reports"))) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .max(Comparator.comparing(path -> path.getFileName().toString()))
                    .get();
        }
    }
}
