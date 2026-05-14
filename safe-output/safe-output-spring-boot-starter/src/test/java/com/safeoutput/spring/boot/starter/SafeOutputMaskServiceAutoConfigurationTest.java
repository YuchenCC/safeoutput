package com.safeoutput.spring.boot.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.safeoutput.core.MaskContext;
import com.safeoutput.core.MaskStrategy;
import com.safeoutput.core.SafeOutputMaskService;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputAutoConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class SafeOutputMaskServiceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SafeOutputAutoConfiguration.class);

    @Test
    void autoConfiguresInjectableMaskServiceForBuiltInTypes() {
        contextRunner.run(context -> {
            SafeOutputMaskService service = context.getBean(SafeOutputMaskService.class);

            assertEquals("138****5678", service.mask("13812345678", "MOBILE"));
            assertEquals("张*", service.mask("张三", "CHINESE_NAME"));
        });
    }

    @Test
    void maskServiceUsesCustomStrategyBean() {
        contextRunner.withUserConfiguration(CustomStrategyConfiguration.class).run(context -> {
            SafeOutputMaskService service = context.getBean(SafeOutputMaskService.class);

            assertEquals("custom-mobile", service.mask("13812345678", "mobileM"));
        });
    }

    @Configuration
    static class CustomStrategyConfiguration {

        @Bean
        MaskStrategy mobileMStrategy() {
            return new MaskStrategy() {
                @Override
                public String type() {
                    return "mobileM";
                }

                @Override
                public String mask(String rawValue, MaskContext context) {
                    return "custom-mobile";
                }
            };
        }
    }
}
