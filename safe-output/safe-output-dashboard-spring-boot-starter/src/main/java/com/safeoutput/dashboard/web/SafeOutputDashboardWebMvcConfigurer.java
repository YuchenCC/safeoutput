package com.safeoutput.dashboard.web;

import com.safeoutput.dashboard.autoconfigure.SafeOutputDashboardProperties;

import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class SafeOutputDashboardWebMvcConfigurer implements WebMvcConfigurer {

    private final SafeOutputDashboardProperties properties;

    public SafeOutputDashboardWebMvcConfigurer(SafeOutputDashboardProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = properties.getPathPrefix();
        registry.addResourceHandler(prefix, prefix + "/", prefix + "/**")
                .addResourceLocations("classpath:/safe-output-dashboard/");
    }
}
