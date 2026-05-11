package com.safeoutput.spring.boot.autoconfigure;

import com.safeoutput.core.MaskStrategy;
import com.safeoutput.core.MaskStrategyRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Safe Output 面向 Spring Boot 2.x 的自动装配入口。
 */
@Configuration
public class SafeOutputAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MaskStrategyRegistry maskStrategyRegistry(ObjectProvider<MaskStrategy> customStrategies) {
        return MaskStrategyRegistry.withBuiltIns(customStrategies.orderedStream()
                .collect(java.util.stream.Collectors.toList()));
    }
}
