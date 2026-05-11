package com.safeoutput.spring.boot.autoconfigure;

import com.safeoutput.core.MaskRule;
import com.safeoutput.core.MaskRuleMatcher;
import com.safeoutput.core.MaskStrategy;
import com.safeoutput.core.MaskStrategyRegistry;
import com.safeoutput.core.SensitiveFieldResolver;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Safe Output 面向 Spring Boot 2.x 的自动装配入口。
 */
@Configuration
@EnableConfigurationProperties(SafeOutputProperties.class)
public class SafeOutputAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MaskStrategyRegistry maskStrategyRegistry(ObjectProvider<MaskStrategy> customStrategies) {
        return MaskStrategyRegistry.withBuiltIns(customStrategies.orderedStream()
                .collect(java.util.stream.Collectors.toList()));
    }

    @Bean
    @ConditionalOnMissingBean
    public MaskRuleMatcher maskRuleMatcher(SafeOutputProperties properties) {
        List<MaskRule> rules = new ArrayList<MaskRule>();
        for (SafeOutputProperties.RuleProperties rule : properties.getRules()) {
            rules.add(MaskRule.configured(rule.getName())
                    .keys(rule.getKeys())
                    .paths(rule.getPaths())
                    .type(rule.getType())
                    .enabled(rule.isEnabled())
                    .build());
        }
        return MaskRuleMatcher.builder()
                .configuredRules(rules)
                .ignoreKeys(properties.getIgnore().getKeys())
                .ignorePaths(properties.getIgnore().getPaths())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SensitiveFieldResolver sensitiveFieldResolver(MaskRuleMatcher maskRuleMatcher) {
        return new SensitiveFieldResolver(maskRuleMatcher);
    }
}
