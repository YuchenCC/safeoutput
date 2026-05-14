package com.safeoutput.core;

public final class MaskRuleRequest {

    private final String key;

    private final String path;

    private final boolean apiIgnored;

    private final String annotationType;

    private final String regexFallbackType;

    private MaskRuleRequest(Builder builder) {
        this.key = builder.key;
        this.path = builder.path;
        this.apiIgnored = builder.apiIgnored;
        this.annotationType = builder.annotationType;
        this.regexFallbackType = builder.regexFallbackType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getKey() {
        return key;
    }

    public String getPath() {
        return path;
    }

    public boolean isApiIgnored() {
        return apiIgnored;
    }

    public String getAnnotationType() {
        return annotationType;
    }

    public String getRegexFallbackType() {
        return regexFallbackType;
    }

    public static final class Builder {

        private String key;

        private String path;

        private boolean apiIgnored;

        private String annotationType = MaskTypes.UNKNOWN;

        private String regexFallbackType = MaskTypes.UNKNOWN;

        private Builder() {
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder apiIgnored(boolean apiIgnored) {
            this.apiIgnored = apiIgnored;
            return this;
        }

        public Builder annotationType(MaskType annotationType) {
            this.annotationType = MaskTypes.from(annotationType);
            return this;
        }

        public Builder annotationType(String annotationType) {
            this.annotationType = MaskTypes.normalize(annotationType);
            return this;
        }

        public Builder regexFallbackType(MaskType regexFallbackType) {
            this.regexFallbackType = MaskTypes.from(regexFallbackType);
            return this;
        }

        public Builder regexFallbackType(String regexFallbackType) {
            this.regexFallbackType = MaskTypes.normalize(regexFallbackType);
            return this;
        }

        public MaskRuleRequest build() {
            return new MaskRuleRequest(this);
        }
    }
}
