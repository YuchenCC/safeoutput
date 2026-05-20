package com.safeoutput.dashboard.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "safe-output.dashboard")
public class SafeOutputDashboardProperties {

    private boolean enabled;

    private String pathPrefix = "/safe-output/dashboard";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPathPrefix() {
        if (pathPrefix == null || pathPrefix.trim().isEmpty()) {
            return "/safe-output/dashboard";
        }
        String normalized = pathPrefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }
}
