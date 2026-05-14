package com.safeoutput.spring.boot.autoconfigure;

import com.safeoutput.core.MaskRule;
import com.safeoutput.core.MaskRuleMatcher;
import com.safeoutput.core.MaskStrategy;
import com.safeoutput.core.MaskStrategyRegistry;
import com.safeoutput.core.ObjectMasker;
import com.safeoutput.core.ObjectMaskerOptions;
import com.safeoutput.core.SensitiveFieldResolver;
import com.safeoutput.report.MaskMetricsCollector;
import com.safeoutput.report.MaskReportExportOptions;
import com.safeoutput.report.MaskReportExporter;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
        // 配置项只负责声明 Rule，具体优先级统一交给 core 的 MaskRuleMatcher 决策。
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

    @Bean
    @ConditionalOnMissingBean
    public ObjectMasker objectMasker(MaskStrategyRegistry maskStrategyRegistry, MaskRuleMatcher maskRuleMatcher,
            SensitiveFieldResolver sensitiveFieldResolver, SafeOutputProperties properties) {
        ObjectMaskerOptions options = ObjectMaskerOptions.builder()
                .maxDepth(properties.getMaxDepth())
                .maxCollectionSize(properties.getMaxCollectionSize())
                .build();
        return new ObjectMasker(maskStrategyRegistry, maskRuleMatcher, sensitiveFieldResolver, options);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "safe-output.report", name = "enabled", havingValue = "true")
    public MaskMetricsCollector maskMetricsCollector() {
        return new MaskMetricsCollector(1000);
    }

    @Bean(destroyMethod = "stop")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "safe-output.report", name = "enabled", havingValue = "true")
    public MaskReportExporter maskReportExporter(SafeOutputProperties properties,
            MaskMetricsCollector collector) {
        SafeOutputProperties.ReportProperties report = properties.getReport();
        MaskReportExporter exporter = new MaskReportExporter(new MaskReportExportOptions(
                Paths.get(report.getDirectory()),
                report.getFilePrefix(),
                report.getIntervalMillis(),
                report.getRetainFiles()), collector);
        exporter.start();
        return exporter;
    }
}
