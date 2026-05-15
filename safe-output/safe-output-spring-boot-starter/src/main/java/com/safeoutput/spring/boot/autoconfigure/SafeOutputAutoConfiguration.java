package com.safeoutput.spring.boot.autoconfigure;

import com.safeoutput.core.DefaultSafeOutputMaskService;
import com.safeoutput.core.MaskEventRecorder;
import com.safeoutput.core.MaskRule;
import com.safeoutput.core.MaskRuleMatcher;
import com.safeoutput.core.MaskStrategy;
import com.safeoutput.core.MaskStrategyRegistry;
import com.safeoutput.core.ObjectMasker;
import com.safeoutput.core.ObjectMaskerOptions;
import com.safeoutput.core.SafeOutputMaskService;
import com.safeoutput.core.SensitiveFieldResolver;
import com.safeoutput.core.UnknownTypeRecorder;
import com.safeoutput.log4j2.SafeOutputLog4j2Runtime;
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
            SensitiveFieldResolver sensitiveFieldResolver, SafeOutputProperties properties,
            ObjectProvider<UnknownTypeRecorder> unknownTypeRecorders,
            ObjectProvider<MaskEventRecorder> maskEventRecorders) {
        // starter 只把 Spring 配置转成 core 选项，递归和 fail-open 语义仍由 ObjectMasker 负责。
        ObjectMaskerOptions options = ObjectMaskerOptions.builder()
                .maxDepth(properties.getMaxDepth())
                .maxCollectionSize(properties.getMaxCollectionSize())
                .build();
        return new ObjectMasker(maskStrategyRegistry, maskRuleMatcher, sensitiveFieldResolver, options,
                unknownTypeRecorders.getIfAvailable(), maskEventRecorders.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public SafeOutputMaskService safeOutputMaskService(MaskStrategyRegistry maskStrategyRegistry,
            ObjectMasker objectMasker, SafeOutputProperties properties,
            ObjectProvider<MaskEventRecorder> maskEventRecorders) {
        return new DefaultSafeOutputMaskService(maskStrategyRegistry, objectMasker,
                properties.getManual().getStrongScan().getTypes(), maskEventRecorders.getIfAvailable());
    }

    @Bean(destroyMethod = "close")
    public SafeOutputLog4j2RuntimeRegistration safeOutputLog4j2RuntimeRegistration(
            MaskRuleMatcher maskRuleMatcher, MaskStrategyRegistry maskStrategyRegistry,
            SafeOutputProperties properties) {
        return new SafeOutputLog4j2RuntimeRegistration(maskRuleMatcher, maskStrategyRegistry, properties);
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

    public static final class SafeOutputLog4j2RuntimeRegistration implements AutoCloseable {

        public SafeOutputLog4j2RuntimeRegistration(MaskRuleMatcher maskRuleMatcher,
                MaskStrategyRegistry maskStrategyRegistry, SafeOutputProperties properties) {
            SafeOutputProperties.LogProperties log = properties.getLog();
            SafeOutputLog4j2Runtime.configure(maskRuleMatcher, maskStrategyRegistry, log.isEnabled(),
                    log.getMaxMessageLength(), log.getMaxValueLength(), log.getRegexFallback().isEnabled(),
                    log.getRegexFallback().isIdCardCheckCodeEnabled(), log.isKeyValueRuleEnabled(),
                    log.getMaxRuleKeys());
        }

        @Override
        public void close() {
            SafeOutputLog4j2Runtime.reset();
        }
    }
}
