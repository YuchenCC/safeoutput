package com.safeoutput.dashboard.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safeoutput.dashboard.web.SafeOutputDashboardController;
import com.safeoutput.dashboard.web.SafeOutputDashboardWebMvcConfigurer;

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
    public SafeOutputDashboardController safeOutputDashboardController(SafeOutputDashboardProperties properties,
            ObjectMapper objectMapper) {
        return new SafeOutputDashboardController(properties, objectMapper);
    }

    @Bean
    public SafeOutputDashboardWebMvcConfigurer safeOutputDashboardWebMvcConfigurer(
            SafeOutputDashboardProperties properties) {
        return new SafeOutputDashboardWebMvcConfigurer(properties);
    }
}
