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
import org.springframework.test.annotation.DirtiesContext;

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
    void integrationGuideCoversBusinessFieldConfigurationSnippets() {
        String guide = restTemplate.getForObject("/demo/integration-guide", String.class);

        assertTrue(guide.contains("yaml-rule"));
        assertTrue(guide.contains("annotation"));
        assertTrue(guide.contains("default-rule"));
        assertTrue(guide.contains("field-ignore"));
        assertTrue(guide.contains("api-ignore"));
        assertTrue(guide.contains("shippingAddress"));
        assertTrue(guide.contains("securityAnswer"));
        assertTrue(guide.contains("@Desensitize"));
        assertTrue(guide.contains("plainNote"));
        assertTrue(guide.contains("/demo/business/customers/*/raw"));
        assertTrue(guide.contains("/demo/business/orders"));
        assertFalse(guide.contains("Log4j2 PatternConverter"));
        assertFalse(guide.contains("SafeOutputMaskService"));
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

        long before = metricsCollector.snapshot().getLogCount();
        collectDemoLogs();
        String ignored = restTemplate.getForObject("/demo/ignored", String.class);
        String exported = restTemplate.getForObject("/demo/report/export", String.class);

        assertTrue(metricsCollector.snapshot().getLogCount() > before);
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
    void maskByTypeEndpointReturnsTwoRoundResultArray() {
        Map<String, String> req = new LinkedHashMap<String, String>();
        req.put("value", "13800138000");
        req.put("type", "MOBILE");
        String result = restTemplate.postForObject("/demo/mask/by-type", req, String.class);

        assertTrue(result.startsWith("["));
        assertTrue(result.contains("\"round\":1"));
        assertTrue(result.contains("\"round\":2"));
        assertTrue(result.contains("\"result\""));
        assertTrue(result.contains("\"elapsedNanos\""));
        assertTrue(result.contains("\"sameAsPrevious\":true"));
        assertTrue(result.contains("138****8000"));
        assertFalse(result.contains("\"iterations\""));
        assertFalse(result.contains("13800138000"));
    }

    @Test
    void maskObjectEndpointReturnsStructuredResult() {
        Map<String, String> req = new LinkedHashMap<String, String>();
        req.put("realName", "李四");
        req.put("mobile", "13900138009");
        req.put("name", "测试商品A");
        String result = restTemplate.postForObject("/demo/mask/object", req, String.class);

        assertTrue(result.startsWith("["));
        assertTrue(result.contains("\"round\":1"));
        assertTrue(result.contains("\"round\":2"));
        assertTrue(result.contains("\"sameAsPrevious\":true"));
        assertTrue(result.contains("\"elapsedNanos\""));
        assertTrue(result.contains("\"realName\":\"李*\""));
        assertTrue(result.contains("\"mobile\":\"139****8009\""));
        assertTrue(result.contains("\"name\":\"测试商品A\""));
        assertFalse(result.contains("\"realName\":\"李四\""));
        assertFalse(result.contains("\"mobile\":\"13900138009\""));
    }

    @Test
    void maskStrongEndpointScansTextAndReturnsResult() {
        Map<String, String> req = new LinkedHashMap<String, String>();
        req.put("text", "手机号13800138000邮箱foo@example.com");
        String result = restTemplate.postForObject("/demo/mask/strong", req, String.class);

        assertTrue(result.startsWith("["));
        assertTrue(result.contains("\"round\":1"));
        assertTrue(result.contains("\"round\":2"));
        assertTrue(result.contains("\"sameAsPrevious\":true"));
        assertTrue(result.contains("\"elapsedNanos\""));
        assertFalse(result.contains("\"iterations\""));
        assertTrue(result.contains("138****8000"));
        assertTrue(result.contains("foo****@example.com"));
    }

    @Test
    void logScenarioEndpointReturnsReadOnlySummariesAndUsesExternalLogSources() {
        long before = metricsCollector.snapshot().getLogCount();
        collectDemoLogs();
        String scenarios = restTemplate.getForObject("/demo/logs/scenarios", String.class);
        ResponseEntity<String> removedTrigger = restTemplate.getForEntity(
                "/demo/logs/scenarios/configured-vs-missing/trigger", String.class);

        assertTrue(scenarios.contains("json-like"));
        assertTrue(scenarios.contains("key-value"));
        assertTrue(scenarios.contains("regex-fallback"));
        assertTrue(metricsCollector.snapshot().getLogCount() > before);
        assertTrue(scenarios.contains("\"templateSummary\""));
        assertTrue(scenarios.contains("\"logRuleSuggestions\""));
        assertTrue(scenarios.contains("certnum"));
        assertTrue(scenarios.contains("mailaddr"));
        assertFalse(scenarios.contains("triggerEndpoint"));
        assertFalse(scenarios.contains("configured-vs-missing"));
        assertTrue(removedTrigger.getStatusCode().is4xxClientError());
        assertFalse(scenarios.contains("11010519491231002X"));
        assertFalse(scenarios.contains("business-detail@example.com"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void businessWorkbenchLogsUseConfiguredKeysWithoutMissingKeySuggestions() {
        long before = metricsCollector.snapshot().getLogCount();
        restTemplate.getForObject("/demo/workbench", String.class);
        restTemplate.getForObject("/demo/business/customer", String.class);
        restTemplate.getForObject("/demo/business/customers", String.class);
        restTemplate.getForObject("/demo/business/customers/C-1001", String.class);
        restTemplate.getForObject("/demo/business/orders", String.class);
        restTemplate.getForObject("/demo/business/orders/ORD-20260518-001", String.class);
        restTemplate.getForObject("/demo/business/payments", String.class);
        restTemplate.getForObject("/demo/business/payments/PAY-8840", String.class);
        restTemplate.getForObject("/demo/business/tickets", String.class);
        restTemplate.getForObject("/demo/business/tickets/TK-20260518-01", String.class);
        restTemplate.getForObject("/demo/business/accounts", String.class);
        restTemplate.getForObject("/demo/business/accounts/AC-7780", String.class);

        String scenarios = restTemplate.getForObject("/demo/logs/scenarios", String.class);

        assertTrue(metricsCollector.snapshot().getLogCount() > before);
        assertFalse(scenarios.contains("mailaddr"));
        assertFalse(scenarios.contains("certnum"));
        assertFalse(scenarios.contains("customer-log@example.com"));
        assertFalse(scenarios.contains("payment-detail@example.com"));
        assertFalse(scenarios.contains("ticket-detail@example.com"));
        assertFalse(scenarios.contains("account-detail@example.com"));
        assertFalse(scenarios.contains("11010519491231002X"));
        assertFalse(scenarios.contains("6222029876543210987"));
    }

    @Test
    void manualMaskDemoEndpointsReturnTwoRoundResults() {
        Map<String, String> byTypeRequest = new LinkedHashMap<String, String>();
        byTypeRequest.put("value", "13800138000");
        byTypeRequest.put("type", "mobileM");
        String byType = restTemplate.postForObject("/demo/mask/by-type", byTypeRequest, String.class);

        Map<String, String> objectRequest = new LinkedHashMap<String, String>();
        objectRequest.put("realName", "王五");
        objectRequest.put("mobile", "13700138008");
        objectRequest.put("name", "手工输入商品");
        String object = restTemplate.postForObject("/demo/mask/object", objectRequest, String.class);

        Map<String, String> strongRequest = new LinkedHashMap<String, String>();
        strongRequest.put("text", "联系 13800138000 foo@example.com");
        String strong = restTemplate.postForObject("/demo/mask/strong", strongRequest, String.class);

        assertTrue(byType.contains("m-138****8000"));
        assertTrue(byType.contains("\"round\":2"));
        assertTrue(byType.contains("\"sameAsPrevious\":true"));
        assertTrue(object.contains("\"realName\":\"王*\""));
        assertTrue(object.contains("\"mobile\":\"137****8008\""));
        assertTrue(object.contains("\"name\":\"手工输入商品\""));
        assertTrue(object.contains("\"sameAsPrevious\":true"));
        assertFalse(object.contains("\"mobile\":\"13700138008\""));
        assertTrue(strong.contains("138****8000"));
        assertTrue(strong.contains("foo****@example.com"));
        assertTrue(strong.contains("\"sameAsPrevious\":true"));

        String snapshot = restTemplate.getForObject("/demo/report/snapshot", String.class);
        assertTrue(snapshot.contains("\"manualCount\""));
        assertFalse(snapshot.contains("13800138000"));
    }

    @Test
    void dashboardEndpointReturnsAggregatedStatsAndChartData() {
        restTemplate.getForObject("/demo/bean", String.class);
        collectDemoLogs();

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
        collectDemoLogs();

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
        assertFalse(logSuggestions.contains("business-detail@example.com"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void logSuggestionsEndpointReturnsYamlForLowConfidenceSuggestions() {
        Map<String, String> byTypeReq = new LinkedHashMap<String, String>();
        byTypeReq.put("value", "13800138000");
        byTypeReq.put("type", "MOBILE");
        restTemplate.postForObject("/demo/mask/by-type", byTypeReq, String.class);

        String logSuggestions = restTemplate.getForObject("/demo/report/log-suggestions", String.class);

        assertTrue(logSuggestions.contains("\"confidence\":\"LOW\""));
        assertTrue(logSuggestions.contains("suggested-certnum"));
        assertTrue(logSuggestions.contains("suggested-mailaddr"));
        assertTrue(logSuggestions.contains("enabled: false"));
        assertFalse(logSuggestions.contains("11010519491231002X"));
        assertFalse(logSuggestions.contains("lab-type@example.com"));
    }

    @Test
    void r2ReportDemoEndpointsReturnRiskAndLogSuggestionsWithoutSensitiveSamples() {
        restTemplate.getForObject("/demo/nested", String.class);
        restTemplate.getForObject("/demo/ignored", String.class);
        collectDemoLogs();

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
        assertFalse(logSuggestions.contains("business-detail@example.com"));
    }

    @Test
    void reportFileCenterExportsListsReadsAndRejectsUnsafeNames() throws Exception {
        restTemplate.getForObject("/demo/business/order", String.class);
        collectDemoLogs();
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

    private void collectDemoLogs() {
        restTemplate.getForObject("/demo/business/customers/C-1001", String.class);
        Map<String, String> byTypeReq = new LinkedHashMap<String, String>();
        byTypeReq.put("value", "13800138000");
        byTypeReq.put("type", "MOBILE");
        restTemplate.postForObject("/demo/mask/by-type", byTypeReq, String.class);
        Map<String, String> strongReq = new LinkedHashMap<String, String>();
        strongReq.put("text", "联系 13600138003 fallback@example.com");
        restTemplate.postForObject("/demo/mask/strong", strongReq, String.class);
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
