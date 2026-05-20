package com.safeoutput.dashboard;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.safeoutput.core.MaskScene;
import com.safeoutput.core.LogRuleSuggestionEvent;
import com.safeoutput.core.ResponseRiskEvent;
import com.safeoutput.report.MaskMetricsCollector;
import com.safeoutput.report.MaskReportExporter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;

@SpringBootTest(classes = TestDashboardApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "safe-output.dashboard.enabled=true",
                "safe-output.report.enabled=true",
                "safe-output.report.directory=target/safe-output-dashboard-test-reports",
                "safe-output.report.file-prefix=dashboard-report",
                "safe-output.report.interval-millis=600000",
                "spring.servlet.multipart.max-file-size=2MB",
                "spring.servlet.multipart.max-request-size=2MB",
                "safe-output.rules[0].keys[0]=phoneNo",
                "safe-output.rules[0].type=MOBILE"
        })
class DashboardWebIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MaskMetricsCollector metricsCollector;

    @Autowired
    private MaskReportExporter reportExporter;

    @Test
    void enabledDashboardExposesStaticEntryAndPostHealthApi() {
        String page = restTemplate.getForObject("/safe-output/dashboard/index.html", String.class);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        ResponseEntity<String> health = restTemplate.postForEntity("/safe-output/dashboard/api/health", body,
                String.class);
        ResponseEntity<String> getHealth = restTemplate.getForEntity("/safe-output/dashboard/api/health",
                String.class);

        assertTrue(page.contains("Safe Output Dashboard"));
        assertTrue(health.getStatusCode().is2xxSuccessful());
        assertTrue(health.getBody().contains("\"enabled\":true"));
        assertTrue(health.getBody().contains("\"pathPrefix\":\"/safe-output/dashboard\""));
        assertTrue(getHealth.getStatusCode().is4xxClientError()
                || getHealth.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void overviewAndResponseRiskExposeAggregatedRuntimeMetricsOnlyThroughPost() {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        counts.put("MOBILE", 2);
        metricsCollector.recordMask(MaskScene.RESPONSE, "MOBILE", 100);
        metricsCollector.recordMask(MaskScene.LOG, "EMAIL", 200);
        metricsCollector.recordMask(MaskScene.MANUAL, "ID_CARD", 300);
        metricsCollector.recordFailure();
        metricsCollector.recordApi(new ResponseRiskEvent("GET", "/customers/1001", "/customers/{id}",
                false, null, false, 2, counts, 9000000));
        metricsCollector.recordApi(new ResponseRiskEvent("GET", "/customers/1001/raw", "/customers/{id}/raw",
                true, "manual reveal", false, 0, new LinkedHashMap<String, Integer>(), 100));

        ResponseEntity<String> overview = restTemplate.postForEntity("/safe-output/dashboard/api/overview",
                new LinkedHashMap<String, Object>(), String.class);
        ResponseEntity<String> risk = restTemplate.postForEntity("/safe-output/dashboard/api/response-risk",
                new LinkedHashMap<String, Object>(), String.class);
        ResponseEntity<String> getOverview = restTemplate.getForEntity("/safe-output/dashboard/api/overview",
                String.class);

        assertTrue(overview.getStatusCode().is2xxSuccessful());
        assertTrue(overview.getBody().contains("\"totalCount\""));
        assertTrue(overview.getBody().contains("\"responseCount\""));
        assertTrue(overview.getBody().contains("\"logCount\""));
        assertTrue(overview.getBody().contains("\"manualCount\""));
        assertTrue(overview.getBody().contains("\"failureCount\""));
        assertTrue(overview.getBody().contains("\"averageElapsedNanos\""));
        assertTrue(overview.getBody().contains("\"maxElapsedNanos\""));
        assertTrue(overview.getBody().contains("\"maskTypeCounts\""));
        assertTrue(overview.getBody().contains("\"topRiskApis\""));
        assertTrue(overview.getBody().contains("\"ignoredRiskApis\""));
        assertTrue(risk.getBody().contains("\"responseRiskSummary\""));
        assertTrue(risk.getBody().contains("\"riskLevel\""));
        assertTrue(risk.getBody().contains("\"ignored\":true"));
        assertTrue(getOverview.getStatusCode().is4xxClientError());
        assertTrue(!overview.getBody().contains("13800138000"));
        assertTrue(!risk.getBody().contains("13800138000"));
    }

    @Test
    void logSuggestionsExposeYamlCandidatesAndFilterConfiguredKeysThroughPost() {
        metricsCollector.record(new LogRuleSuggestionEvent("certNum", "ID_CARD", "certNum=ID_CARD",
                System.currentTimeMillis()));
        metricsCollector.record(new LogRuleSuggestionEvent("certNum", "ID_CARD", "certNum=ID_CARD",
                System.currentTimeMillis()));
        metricsCollector.record(new LogRuleSuggestionEvent("phoneNo", "MOBILE", "phoneNo=MOBILE",
                System.currentTimeMillis()));

        ResponseEntity<String> suggestions = restTemplate.postForEntity(
                "/safe-output/dashboard/api/log-suggestions", new LinkedHashMap<String, Object>(), String.class);
        ResponseEntity<String> getSuggestions = restTemplate.getForEntity(
                "/safe-output/dashboard/api/log-suggestions", String.class);

        assertTrue(suggestions.getStatusCode().is2xxSuccessful());
        assertTrue(suggestions.getBody().contains("\"logRuleSuggestions\""));
        assertTrue(suggestions.getBody().contains("\"configSnippet\""));
        assertTrue(suggestions.getBody().contains("\"key\":\"certnum\""));
        assertTrue(suggestions.getBody().contains("\"suggestedType\":\"id_card\""));
        assertTrue(suggestions.getBody().contains("\"confidence\":\"MEDIUM\""));
        assertTrue(suggestions.getBody().contains("enabled: false"));
        assertTrue(!suggestions.getBody().contains("phoneNo"));
        assertTrue(!suggestions.getBody().contains("11010519491231002X"));
        assertTrue(getSuggestions.getStatusCode().is4xxClientError());
    }

    @Test
    void reportDirectoryListsAndViewsReportsByPostBodyWithSafeFilenameChecks() throws Exception {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        counts.put("EMAIL", 1);
        metricsCollector.recordMask(MaskScene.RESPONSE, "EMAIL", 100);
        metricsCollector.recordApi(new ResponseRiskEvent("GET", "/orders/1001", "/orders/{id}",
                false, null, false, 1, counts, 100));
        Path exported = reportExporter.exportNow();
        Files.write(Paths.get("target/safe-output-dashboard-test-reports/dashboard-report.txt"),
                "not-json".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("filename", exported.getFileName().toString());
        ResponseEntity<String> files = restTemplate.postForEntity("/safe-output/dashboard/api/reports/list",
                new LinkedHashMap<String, Object>(), String.class);
        ResponseEntity<String> dashboard = restTemplate.postForEntity("/safe-output/dashboard/api/reports/view",
                request, String.class);
        Map<String, Object> traversalRequest = new LinkedHashMap<String, Object>();
        traversalRequest.put("filename", "../pom.xml");
        ResponseEntity<String> traversal = restTemplate.postForEntity("/safe-output/dashboard/api/reports/view",
                traversalRequest, String.class);
        Map<String, Object> nonJsonRequest = new LinkedHashMap<String, Object>();
        nonJsonRequest.put("filename", "dashboard-report.txt");
        ResponseEntity<String> nonJson = restTemplate.postForEntity("/safe-output/dashboard/api/reports/view",
                nonJsonRequest, String.class);
        ResponseEntity<String> getView = restTemplate.getForEntity("/safe-output/dashboard/api/reports/view",
                String.class);

        assertTrue(files.getStatusCode().is2xxSuccessful());
        assertTrue(files.getBody().contains("\"count\""));
        assertTrue(files.getBody().contains("\"viewable\":true"));
        assertTrue(files.getBody().contains(exported.getFileName().toString()));
        assertTrue(dashboard.getStatusCode().is2xxSuccessful());
        assertTrue(dashboard.getBody().contains("\"filename\""));
        assertTrue(dashboard.getBody().contains("\"totalCount\""));
        assertTrue(dashboard.getBody().contains("\"topRiskApis\""));
        assertTrue(dashboard.getBody().contains("\"maskedFieldCount\""));
        assertTrue(traversal.getStatusCode().is4xxClientError());
        assertTrue(nonJson.getStatusCode().is4xxClientError());
        assertTrue(getView.getStatusCode().is4xxClientError());
        assertTrue(!dashboard.getBody().contains("foo@example.com"));
    }

    @Test
    void uploadedReportReturnsTemporaryDashboardWithoutWritingReportDirectory() throws Exception {
        metricsCollector.recordMask(MaskScene.RESPONSE, "MOBILE", 100);
        Path exported = reportExporter.exportNow();
        String json = new String(Files.readAllBytes(exported), StandardCharsets.UTF_8);
        String before = restTemplate.postForObject("/safe-output/dashboard/api/reports/list",
                new LinkedHashMap<String, Object>(), String.class);

        ResponseEntity<String> uploaded = restTemplate.postForEntity("/safe-output/dashboard/api/reports/upload",
                multipart("upload.json", json.getBytes(StandardCharsets.UTF_8)), String.class);
        ResponseEntity<String> textFile = restTemplate.postForEntity("/safe-output/dashboard/api/reports/upload",
                multipart("upload.txt", json.getBytes(StandardCharsets.UTF_8)), String.class);
        ResponseEntity<String> tooLarge = restTemplate.postForEntity("/safe-output/dashboard/api/reports/upload",
                multipart("large.json", new byte[1024 * 1024 + 1]), String.class);
        ResponseEntity<String> badShape = restTemplate.postForEntity("/safe-output/dashboard/api/reports/upload",
                multipart("bad.json", "{}".getBytes(StandardCharsets.UTF_8)), String.class);
        String after = restTemplate.postForObject("/safe-output/dashboard/api/reports/list",
                new LinkedHashMap<String, Object>(), String.class);

        assertTrue(uploaded.getStatusCode().is2xxSuccessful());
        assertTrue(uploaded.getBody().contains("\"filename\":\"upload.json\""));
        assertTrue(uploaded.getBody().contains("\"totalCount\""));
        assertTrue(textFile.getStatusCode().is4xxClientError());
        assertTrue(tooLarge.getStatusCode() == HttpStatus.PAYLOAD_TOO_LARGE);
        assertTrue(badShape.getStatusCode().is4xxClientError());
        assertTrue(before.equals(after));
        assertTrue(!after.contains("upload.json"));
        assertTrue(!uploaded.getBody().contains("13800138000"));
    }

    private static HttpEntity<MultiValueMap<String, Object>> multipart(String filename, byte[] content) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("file", new NamedByteArrayResource(filename, content));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<MultiValueMap<String, Object>>(body, headers);
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        NamedByteArrayResource(String filename, byte[] byteArray) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
