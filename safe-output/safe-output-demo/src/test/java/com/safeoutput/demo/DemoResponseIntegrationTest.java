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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    void r25BusinessWorkbenchUsesBusinessScenariosAndResponseAdvice() {
        String workbench = restTemplate.getForObject("/demo/workbench", String.class);
        String customer = restTemplate.getForObject("/demo/business/customer", String.class);
        String order = restTemplate.getForObject("/demo/business/order", String.class);
        String payment = restTemplate.getForObject("/demo/business/payment", String.class);
        String tickets = restTemplate.getForObject("/demo/business/tickets", String.class);
        String account = restTemplate.getForObject("/demo/business/account", String.class);
        String customers = restTemplate.getForObject("/demo/business/customers", String.class);
        String customerDetail = restTemplate.getForObject("/demo/business/customers/C-1001", String.class);
        String orderDetail = restTemplate.getForObject("/demo/business/orders/ORD-20260518-001", String.class);
        String paymentDetail = restTemplate.getForObject("/demo/business/payments/PAY-8840", String.class);
        String ticketDetail = restTemplate.getForObject("/demo/business/tickets/TK-20260518-01", String.class);
        String accountDetail = restTemplate.getForObject("/demo/business/accounts/AC-7780", String.class);

        assertTrue(workbench.contains("客户档案"));
        assertTrue(workbench.contains("订单履约"));
        assertTrue(workbench.contains("支付核验"));
        assertTrue(workbench.contains("工单处理"));
        assertTrue(workbench.contains("账户安全"));
        assertTrue(customer.contains("\"displayName\":\"张*\""));
        assertTrue(customer.contains("138****8000"));
        assertTrue(customer.contains("110105********002X"));
        assertTrue(customer.contains("cus****@example.com"));
        assertTrue(customer.contains("北京市核心区****"));
        assertTrue(customer.contains("\"plainNote\":\"demo note 13800138000\""));
        assertFalse(customer.contains("\"mobile\":\"13800138000\""));
        assertTrue(order.contains("622202*********0123"));
        assertTrue(payment.contains("\"securityAnswer\":\"****\""));
        assertTrue(tickets.contains("\"requesterName\":\"用*1\""));
        assertTrue(account.contains("\"password\":\"********\""));
        assertFalse(order.contains("6222021234567890123"));
        assertFalse(payment.contains("13900138001"));
        assertFalse(tickets.contains("\"mobile\":\"13700138002\""));
        assertFalse(account.contains("Secret-12345"));
        assertTrue(customers.contains("\"customerNo\":\"C-1001\""));
        assertTrue(customerDetail.contains("138****8000"));
        assertTrue(orderDetail.contains("622202*********0120"));
        assertTrue(paymentDetail.contains("\"securityAnswer\":\"****\""));
        assertTrue(ticketDetail.contains("\"requesterName\":\"用*1\""));
        assertTrue(accountDetail.contains("\"password\":\"********\""));
        assertFalse(customerDetail.contains("\"mobile\":\"13800138000\""));
        assertFalse(orderDetail.contains("6222021234567890120"));
        assertFalse(paymentDetail.contains("13900138000"));
        assertFalse(ticketDetail.contains("\"mobile\":\"13700138000\""));
        assertFalse(accountDetail.contains("Secret-12340"));
    }

    @Test
    void r25BusinessApiIgnoreKeepsPlaintextAndRecordsRiskMetric() {
        String ignored = restTemplate.getForObject("/demo/business/legacy-plaintext", String.class);
        String rawCustomer = restTemplate.getForObject("/demo/business/customers/C-1001/raw", String.class);
        String rawOrder = restTemplate.getForObject("/demo/business/orders/ORD-20260518-001/raw", String.class);
        String rawPayment = restTemplate.getForObject("/demo/business/payments/PAY-8840/raw", String.class);
        String rawTicket = restTemplate.getForObject("/demo/business/tickets/TK-20260518-01/raw", String.class);
        String rawAccount = restTemplate.getForObject("/demo/business/accounts/AC-7780/raw", String.class);

        assertTrue(ignored.contains("\"mobile\":\"13800138000\""));
        assertTrue(rawCustomer.contains("\"mobile\":\"13800138000\""));
        assertTrue(rawOrder.contains("6222021234567890120"));
        assertTrue(rawPayment.contains("\"securityAnswer\":\"母亲生日是19900100\""));
        assertTrue(rawTicket.contains("13700138000"));
        assertTrue(rawAccount.contains("Secret-12340"));
        ApiMaskMetrics metric = metricsCollector.snapshot().getApiMetric("GET", "/demo/business/legacy-plaintext");
        ApiMaskMetrics rawMetric = metricsCollector.snapshot().getApiMetric("GET", "/demo/business/customers/{id}/raw");
        assertNotNull(metric);
        assertNotNull(rawMetric);
        assertTrue(metric.isIgnored());
        assertTrue(rawMetric.isIgnored());
    }

    @Test
    void integrationGuideCoversAllMajorIntegrationModes() {
        String guide = restTemplate.getForObject("/demo/integration-guide", String.class);

        assertTrue(guide.contains("yaml-rule"));
        assertTrue(guide.contains("annotation"));
        assertTrue(guide.contains("default-rule"));
        assertTrue(guide.contains("field-ignore"));
        assertTrue(guide.contains("api-ignore"));
        assertTrue(guide.contains("log4j2"));
        assertTrue(guide.contains("manual"));
        assertTrue(guide.contains("/demo/business/customer"));
        assertTrue(guide.contains("/demo/logs/scenarios"));
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
        assertFalse(json.contains("13900139000"));
        assertFalse(json.contains("foo@example.com"));
        assertFalse(json.contains("bar@example.com"));
        assertFalse(json.contains("11010519491231002X"));
        assertFalse(json.contains("6222021234567890123"));
    }

    @Test
    void maskByTypeEndpointReturnsFirstSecondAndIdempotentFields() {
        Map<String, String> req = new LinkedHashMap<String, String>();
        req.put("value", "13800138000");
        req.put("type", "MOBILE");
        req.put("iterations", "3");
        String result = restTemplate.postForObject("/demo/mask/by-type", req, String.class);

        assertTrue(result.contains("\"first\""));
        assertTrue(result.contains("\"second\""));
        assertTrue(result.contains("\"idempotent\""));
        assertTrue(result.contains("\"iterations\":3"));
        assertTrue(result.contains("\"totalElapsedNanos\""));
        assertTrue(result.contains("\"averageElapsedNanos\""));
        assertTrue(result.contains("138****8000"));
        assertFalse(result.contains("13800138000"));
    }

    @Test
    void maskObjectEndpointReturnsStructuredResult() {
        String result = restTemplate.postForObject(
                "/demo/mask/object", new LinkedHashMap<String, String>(), String.class);

        assertTrue(result.contains("\"first\""));
        assertTrue(result.contains("\"second\""));
        assertTrue(result.contains("\"idempotent\":true"));
        assertTrue(result.contains("\"realName\""));
        assertTrue(result.contains("\"mobile\""));
    }

    @Test
    void maskStrongEndpointScansTextAndReturnsResult() {
        Map<String, String> req = new LinkedHashMap<String, String>();
        req.put("text", "手机号13800138000邮箱foo@example.com");
        req.put("iterations", "2");
        String result = restTemplate.postForObject("/demo/mask/strong", req, String.class);

        assertTrue(result.contains("\"first\""));
        assertTrue(result.contains("\"second\""));
        assertTrue(result.contains("\"idempotent\":true"));
        assertTrue(result.contains("\"iterations\":2"));
        assertTrue(result.contains("138****8000"));
        assertTrue(result.contains("foo****@example.com"));
    }

    @Test
    void logScenarioEndpointsTriggerRealLog4j2AndReturnOnlyTemplateSummaries() {
        String scenarios = restTemplate.getForObject("/demo/logs/scenarios", String.class);
        long before = metricsCollector.snapshot().getLogCount();
        String triggered = restTemplate.getForObject("/demo/logs/scenarios/configured-vs-missing/trigger",
                String.class);

        assertTrue(scenarios.contains("json-like"));
        assertTrue(scenarios.contains("key-value"));
        assertTrue(scenarios.contains("regex-fallback"));
        assertTrue(scenarios.contains("configured-vs-missing"));
        assertTrue(metricsCollector.snapshot().getLogCount() > before);
        assertTrue(triggered.contains("\"templateSummary\""));
        assertTrue(triggered.contains("\"logRuleSuggestions\""));
        assertTrue(triggered.contains("certnum"));
        assertTrue(triggered.contains("mailaddr"));
        assertFalse(triggered.contains("13500138004"));
        assertFalse(triggered.contains("11010519491231002X"));
        assertFalse(triggered.contains("missing@example.com"));
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
    void dashboardEndpointReturnsAggregatedStatsAndChartData() {
        restTemplate.getForObject("/demo/bean", String.class);
        restTemplate.getForObject("/demo/logs", String.class);
        Map<String, String> byTypeReq = new LinkedHashMap<String, String>();
        byTypeReq.put("value", "13800138000");
        byTypeReq.put("type", "mobileM");
        restTemplate.postForObject("/demo/mask/by-type", byTypeReq, String.class);

        String dashboard = restTemplate.getForObject("/demo/report/dashboard", String.class);

        assertTrue(metricsCollector.snapshot().getLogCount() > 0);
        assertTrue(dashboard.contains("\"totalCount\""));
        assertTrue(dashboard.contains("\"responseCount\""));
        assertTrue(dashboard.contains("\"logCount\""));
        assertTrue(dashboard.contains("\"manualCount\""));
        assertTrue(dashboard.contains("\"highRiskApiCount\""));
        assertTrue(dashboard.contains("\"suggestionCount\""));
        assertTrue(dashboard.contains("\"averageElapsedNanos\""));
        assertTrue(dashboard.contains("\"maskTypeCounts\""));
        assertTrue(dashboard.contains("\"topRiskApis\""));
        assertTrue(dashboard.contains("\"sceneTrend\""));
        assertTrue(dashboard.contains("\"suggestionCount\":2"));
        assertFalse(dashboard.contains("13800138000"));
    }

    @Test
    void responseRiskEndpointReturnsDetailedProfileFields() {
        restTemplate.getForObject("/demo/nested", String.class);
        restTemplate.getForObject("/demo/ignored", String.class);

        String risk = restTemplate.getForObject("/demo/report/response-risk", String.class);

        assertTrue(risk.contains("\"riskLevel\""));
        assertTrue(risk.contains("\"riskScore\""));
        assertTrue(risk.contains("\"averageElapsedNanos\""));
        assertTrue(risk.contains("\"slowMaskCount\""));
        assertTrue(risk.contains("\"maskTypeCounts\""));
        assertTrue(risk.contains("\"ignored\""));
        assertFalse(risk.contains("13800138000"));
    }

    @Test
    void logSuggestionsEndpointReturnsYamlSnippet() {
        restTemplate.getForObject("/demo/logs", String.class);

        String logSuggestions = restTemplate.getForObject("/demo/report/log-suggestions", String.class);

        assertTrue(logSuggestions.contains("\"configSnippet\""));
        assertTrue(logSuggestions.contains("safe-output"));
        assertTrue(logSuggestions.contains("keys"));
        assertTrue(logSuggestions.contains("type"));
        assertTrue(logSuggestions.contains("suggestedType"));
        assertTrue(logSuggestions.contains("hitCount"));
        assertTrue(logSuggestions.contains("confidence"));
        assertTrue(logSuggestions.contains("effectScopes"));
        assertFalse(logSuggestions.contains("phoneno"));
        assertTrue(logSuggestions.contains("certnum"));
        assertTrue(logSuggestions.contains("mailaddr"));
        assertFalse(logSuggestions.contains("13800138000"));
        assertFalse(logSuggestions.contains("13900139000"));
        assertFalse(logSuggestions.contains("bar@example.com"));
    }

    @Test
    void r2ReportDemoEndpointsReturnRiskAndLogSuggestionsWithoutSensitiveSamples() {
        restTemplate.getForObject("/demo/nested", String.class);
        restTemplate.getForObject("/demo/ignored", String.class);
        restTemplate.getForObject("/demo/logs", String.class);

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
        assertFalse(logSuggestions.contains("phoneno"));
        assertTrue(logSuggestions.contains("certnum"));
        assertTrue(logSuggestions.contains("mailaddr"));
        assertFalse(responseRisk.contains("13800138000"));
        assertFalse(responseRisk.contains("6222021234567890123"));
        assertFalse(logSuggestions.contains("13800138000"));
        assertFalse(logSuggestions.contains("13900139000"));
        assertFalse(logSuggestions.contains("11010519491231002X"));
        assertFalse(logSuggestions.contains("foo@example.com"));
        assertFalse(logSuggestions.contains("bar@example.com"));
    }

    @Test
    void reportFileCenterExportsListsReadsAndRejectsUnsafeNames() throws Exception {
        restTemplate.getForObject("/demo/business/order", String.class);
        restTemplate.getForObject("/demo/logs/scenarios/configured-vs-missing/trigger", String.class);
        String exported = restTemplate.getForObject("/demo/report/export", String.class);
        assertTrue(exported.contains("demo-report"));

        String files = restTemplate.getForObject("/demo/report/files", String.class);
        Path latest = latestReport();
        String name = latest.getFileName().toString();
        String raw = restTemplate.getForObject("/demo/report/files/" + name, String.class);
        String dashboard = restTemplate.getForObject("/demo/report/files/" + name + "/dashboard", String.class);
        ResponseEntity<String> traversal = restTemplate.getForEntity("/demo/report/files/../pom.xml",
                String.class);
        ResponseEntity<String> nonJson = restTemplate.getForEntity("/demo/report/files/demo-report.txt",
                String.class);

        assertTrue(files.contains("\"count\""));
        assertTrue(files.contains(name));
        assertTrue(files.contains("\"size\""));
        assertTrue(raw.contains("\"totalCount\""));
        assertTrue(dashboard.contains("\"responseCount\""));
        assertTrue(dashboard.contains("\"maskTypeCounts\""));
        assertTrue(dashboard.contains("\"topRiskApis\""));
        assertTrue(dashboard.contains("\"ignoredRiskApis\""));
        assertTrue(dashboard.contains("\"logRuleSuggestions\""));
        assertTrue(traversal.getStatusCode().is4xxClientError() || traversal.getStatusCode() == HttpStatus.NOT_FOUND);
        assertTrue(nonJson.getStatusCode().is4xxClientError());
        assertFalse(raw.contains("13800138000"));
        assertFalse(dashboard.contains("11010519491231002X"));
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
