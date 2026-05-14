package com.safeoutput.spring.boot.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.safeoutput.core.MaskContext;
import com.safeoutput.core.MaskResult;
import com.safeoutput.core.MaskStrategy;
import com.safeoutput.core.MaskStrategyRegistry;
import com.safeoutput.core.MaskType;
import com.safeoutput.core.MaskTypes;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputAutoConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class SafeOutputAutoConfigurationTest {

    @Test
    void autoConfigurationExposesRegistryWithBuiltInStrategies() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                SafeOutputAutoConfiguration.class);
        try {
            MaskStrategyRegistry registry = context.getBean(MaskStrategyRegistry.class);

            assertTrue(registry.find(MaskType.MOBILE).isPresent());
        } finally {
            context.close();
        }
    }

    @Test
    void autoConfigurationRegistersCustomStrategyBeansAfterBuiltIns() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                SafeOutputAutoConfiguration.class,
                CustomStrategyConfiguration.class);
        try {
            MaskStrategy customStrategy = context.getBean("customEmailStrategy", MaskStrategy.class);
            MaskStrategyRegistry registry = context.getBean(MaskStrategyRegistry.class);

            assertSame(customStrategy, registry.find(MaskType.EMAIL).get());
            MaskResult result = registry.find(MaskType.EMAIL).get().apply(MaskContext.builder()
                    .maskType(MaskType.EMAIL)
                    .rawValue("zhangsan@example.com")
                    .build());
            assertEquals("custom-email", result.getValue());
        } finally {
            context.close();
        }
    }

    @Configuration
    static class CustomStrategyConfiguration {

        @Bean
        MaskStrategy customEmailStrategy() {
            return new MaskStrategy() {
                @Override
                public String type() {
                    return MaskTypes.EMAIL;
                }

                @Override
                public String mask(String rawValue, MaskContext context) {
                    return "custom-email";
                }
            };
        }
    }
}
