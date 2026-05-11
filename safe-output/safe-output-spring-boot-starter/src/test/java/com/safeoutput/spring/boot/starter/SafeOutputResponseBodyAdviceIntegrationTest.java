package com.safeoutput.spring.boot.starter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.safeoutput.spring.boot.autoconfigure.SafeOutputAutoConfiguration;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputMvcAutoConfiguration;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputResponseBodyAdvice;
import com.safeoutput.core.ResponseRiskEvent;
import com.safeoutput.core.ResponseRiskRecorder;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class SafeOutputResponseBodyAdviceIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SafeOutputAutoConfiguration.class, SafeOutputMvcAutoConfiguration.class);

    @Test
    void responseAdviceMasksBeanMapListAndResponseEntity() {
        contextRunner.run(context -> {
            MockMvc mvc = mvc(context.getBean(SafeOutputResponseBodyAdvice.class));

            mvc.perform(get("/bean"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("{\"mobile\":\"138****5678\"}"));
            mvc.perform(get("/map"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("{\"email\":\"ali****@example.com\"}"));
            mvc.perform(get("/list"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("[{\"mobile\":\"139****5678\"}]"));
            mvc.perform(get("/entity"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("{\"password\":\"********\"}"));
        });
    }

    @Test
    void disabledGlobalOrResponseSceneLeavesBodyUnchanged() {
        contextRunner.withPropertyValues("safe-output.enabled=false")
                .run(context -> mvc(context.getBean(SafeOutputResponseBodyAdvice.class))
                        .perform(get("/bean"))
                        .andExpect(status().isOk())
                        .andExpect(content().string("{\"mobile\":\"13812345678\"}")));

        contextRunner.withPropertyValues("safe-output.response.enabled=false")
                .run(context -> mvc(context.getBean(SafeOutputResponseBodyAdvice.class))
                        .perform(get("/bean"))
                        .andExpect(status().isOk())
                        .andExpect(content().string("{\"mobile\":\"13812345678\"}")));
    }

    @Test
    void apiIgnoreSkipsResponseMaskingAndRecordsRiskEvent() {
        contextRunner
                .withUserConfiguration(RecordingConfiguration.class)
                .withPropertyValues(
                        "safe-output.ignore.apis[0].method=GET",
                        "safe-output.ignore.apis[0].path=/api/raw/**",
                        "safe-output.ignore.apis[0].reason=business plaintext lookup")
                .run(context -> {
                    mvc(context.getBean(SafeOutputResponseBodyAdvice.class))
                            .perform(get("/api/raw/mobile"))
                            .andExpect(status().isOk())
                            .andExpect(content().string("{\"mobile\":\"13812345678\"}"));

                    ResponseRiskEvent event = context.getBean(RecordingResponseRiskRecorder.class).lastEvent.get();
                    org.junit.jupiter.api.Assertions.assertEquals("GET", event.getMethod());
                    org.junit.jupiter.api.Assertions.assertEquals("/api/raw/mobile", event.getPath());
                    org.junit.jupiter.api.Assertions.assertEquals(true, event.isIgnored());
                    org.junit.jupiter.api.Assertions.assertEquals("business plaintext lookup", event.getIgnoreReason());
                });
    }

    @Test
    void adviceFailsOpenWhenMaskingThrows() throws Exception {
        SafeOutputResponseBodyAdvice advice = new SafeOutputResponseBodyAdvice(null, null);

        mvc(advice).perform(get("/bean"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"mobile\":\"13812345678\"}"));
    }

    private static MockMvc mvc(Object advice) {
        return MockMvcBuilders.standaloneSetup(new DemoController())
                .setControllerAdvice(advice)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @RestController
    private static final class DemoController {

        @GetMapping("/bean")
        CustomerResponse bean() {
            return new CustomerResponse("13812345678");
        }

        @GetMapping("/map")
        Map<String, Object> map() {
            Map<String, Object> value = new LinkedHashMap<String, Object>();
            value.put("email", "alice@example.com");
            return value;
        }

        @GetMapping("/list")
        List<Map<String, Object>> list() {
            Map<String, Object> value = new LinkedHashMap<String, Object>();
            value.put("mobile", "13912345678");
            return Arrays.asList(value);
        }

        @GetMapping("/entity")
        ResponseEntity<Map<String, Object>> entity() {
            Map<String, Object> value = new LinkedHashMap<String, Object>();
            value.put("password", "secret-value");
            return ResponseEntity.ok(value);
        }

        @GetMapping("/api/raw/mobile")
        CustomerResponse rawMobile() {
            return new CustomerResponse("13812345678");
        }
    }

    private static final class CustomerResponse {

        private final String mobile;

        private CustomerResponse(String mobile) {
            this.mobile = mobile;
        }

        @SuppressWarnings("unused")
        public String getMobile() {
            return mobile;
        }
    }

    @Configuration
    static class RecordingConfiguration {

        @Bean
        RecordingResponseRiskRecorder recordingResponseRiskRecorder() {
            return new RecordingResponseRiskRecorder();
        }
    }

    private static final class RecordingResponseRiskRecorder implements ResponseRiskRecorder {

        private final AtomicReference<ResponseRiskEvent> lastEvent = new AtomicReference<ResponseRiskEvent>();

        @Override
        public void record(ResponseRiskEvent event) {
            lastEvent.set(event);
        }
    }
}
