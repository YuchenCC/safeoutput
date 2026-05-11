package com.safeoutput.spring.boot.autoconfigure;

import com.safeoutput.core.MaskType;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Safe Output 对外配置属性。
 */
@ConfigurationProperties(prefix = "safe-output")
public class SafeOutputProperties {

    private boolean enabled = true;

    private String maskChar = "*";

    private int maxDepth = 8;

    private int maxCollectionSize = 1000;

    private final SceneProperties response = new SceneProperties();

    private final LogProperties log = new LogProperties();

    private final List<RuleProperties> rules = new ArrayList<RuleProperties>();

    private final IgnoreProperties ignore = new IgnoreProperties();

    private final ReportProperties report = new ReportProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMaskChar() {
        return maskChar;
    }

    public void setMaskChar(String maskChar) {
        this.maskChar = maskChar;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getMaxCollectionSize() {
        return maxCollectionSize;
    }

    public void setMaxCollectionSize(int maxCollectionSize) {
        this.maxCollectionSize = maxCollectionSize;
    }

    public SceneProperties getResponse() {
        return response;
    }

    public LogProperties getLog() {
        return log;
    }

    public List<RuleProperties> getRules() {
        return rules;
    }

    public IgnoreProperties getIgnore() {
        return ignore;
    }

    public ReportProperties getReport() {
        return report;
    }

    public static class SceneProperties {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class RuleProperties {

        private String name;

        private final List<String> keys = new ArrayList<String>();

        private final List<String> paths = new ArrayList<String>();

        private MaskType type = MaskType.UNKNOWN;

        private boolean enabled = true;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getKeys() {
            return keys;
        }

        public List<String> getPaths() {
            return paths;
        }

        public MaskType getType() {
            return type;
        }

        public void setType(MaskType type) {
            this.type = type;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class IgnoreProperties {

        private final List<String> keys = new ArrayList<String>();

        private final List<String> paths = new ArrayList<String>();

        private final List<String> packages = new ArrayList<String>();

        private final List<String> classes = new ArrayList<String>();

        private final List<ApiIgnoreProperties> apis = new ArrayList<ApiIgnoreProperties>();

        public List<String> getKeys() {
            return keys;
        }

        public List<String> getPaths() {
            return paths;
        }

        public List<String> getPackages() {
            return packages;
        }

        public List<String> getClasses() {
            return classes;
        }

        public List<ApiIgnoreProperties> getApis() {
            return apis;
        }
    }

    public static class ApiIgnoreProperties {

        private String pattern;

        private String reason;

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class ReportProperties {

        private boolean enabled;

        private boolean includeApiMetrics = true;

        private boolean includeFieldPath = true;

        private boolean includeRawValue;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isIncludeApiMetrics() {
            return includeApiMetrics;
        }

        public void setIncludeApiMetrics(boolean includeApiMetrics) {
            this.includeApiMetrics = includeApiMetrics;
        }

        public boolean isIncludeFieldPath() {
            return includeFieldPath;
        }

        public void setIncludeFieldPath(boolean includeFieldPath) {
            this.includeFieldPath = includeFieldPath;
        }

        public boolean isIncludeRawValue() {
            return includeRawValue;
        }

        public void setIncludeRawValue(boolean includeRawValue) {
            this.includeRawValue = includeRawValue;
        }
    }

    public static class LogProperties extends SceneProperties {

        private String framework = "LOG4J2";

        private int maxMessageLength = 5000;

        private int maxValueLength = 300;

        private final RegexFallbackProperties regexFallback = new RegexFallbackProperties();

        public String getFramework() {
            return framework;
        }

        public void setFramework(String framework) {
            this.framework = framework;
        }

        public int getMaxMessageLength() {
            return maxMessageLength;
        }

        public void setMaxMessageLength(int maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
        }

        public int getMaxValueLength() {
            return maxValueLength;
        }

        public void setMaxValueLength(int maxValueLength) {
            this.maxValueLength = maxValueLength;
        }

        public RegexFallbackProperties getRegexFallback() {
            return regexFallback;
        }
    }

    public static class RegexFallbackProperties {

        private boolean enabled;

        private final List<MaskType> types = new ArrayList<MaskType>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<MaskType> getTypes() {
            return types;
        }
    }
}
