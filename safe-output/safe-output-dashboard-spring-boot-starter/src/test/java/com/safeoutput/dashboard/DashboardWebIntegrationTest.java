package com.safeoutput.dashboard;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(classes = TestDashboardApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "safe-output.dashboard.enabled=true")
class DashboardWebIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

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
}
