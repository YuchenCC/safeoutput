package com.safeoutput.dashboard.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safeoutput.dashboard.service.SafeOutputDashboardAssembler;
import com.safeoutput.dashboard.service.SafeOutputDashboardReportFileStore;
import com.safeoutput.dashboard.web.SafeOutputDashboardController;
import com.safeoutput.dashboard.web.SafeOutputDashboardWebMvcConfigurer;
import com.safeoutput.core.SafeOutputMaskService;
import com.safeoutput.report.MaskMetricsCollector;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

@Configuration
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "safe-output.dashboard", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SafeOutputDashboardProperties.class)
public class SafeOutputDashboardAutoConfiguration {

    @Bean
    public SafeOutputDashboardAssembler safeOutputDashboardAssembler() {
        return new SafeOutputDashboardAssembler();
    }

    @Bean
    public SafeOutputDashboardReportFileStore safeOutputDashboardReportFileStore(
            ObjectProvider<SafeOutputProperties> safeOutputProperties) {
        return new SafeOutputDashboardReportFileStore(safeOutputProperties);
    }

    @Bean
    public SafeOutputDashboardController safeOutputDashboardController(
            SafeOutputDashboardProperties properties,
            ObjectMapper objectMapper,
            SafeOutputDashboardAssembler dashboardAssembler,
            ObjectProvider<MaskMetricsCollector> metricsCollectors,
            ObjectProvider<SafeOutputProperties> safeOutputProperties,
            SafeOutputDashboardReportFileStore reportFileStore,
            ObjectProvider<SafeOutputMaskService> maskServices) {
        return new SafeOutputDashboardController(properties, objectMapper, dashboardAssembler, metricsCollectors,
                safeOutputProperties, reportFileStore, maskServices);
    }

    @Bean
    public SafeOutputDashboardWebMvcConfigurer safeOutputDashboardWebMvcConfigurer(
            SafeOutputDashboardProperties properties) {
        return new SafeOutputDashboardWebMvcConfigurer(properties);
    }
}
