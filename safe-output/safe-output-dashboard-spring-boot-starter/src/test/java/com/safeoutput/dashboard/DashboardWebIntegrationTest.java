package com.safeoutput.dashboard;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.safeoutput.core.MaskScene;
import com.safeoutput.core.LogRuleSuggestionEvent;
import com.safeoutput.core.ResponseRiskEvent;
import com.safeoutput.report.MaskMetricsCollector;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(classes = TestDashboardApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "safe-output.dashboard.enabled=true",
                "safe-output.report.enabled=true",
                "safe-output.rules[0].keys[0]=phoneNo",
                "safe-output.rules[0].type=MOBILE"
        })
class DashboardWebIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MaskMetricsCollector metricsCollector;

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
}
