package com.safeoutput.core;

public final class MaskRuleRequest {

    private final String key;

    private final String path;

    private final boolean apiIgnored;

    private final MaskType annotationType;

    private final MaskType regexFallbackType;

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

    public MaskType getAnnotationType() {
        return annotationType;
    }

    public MaskType getRegexFallbackType() {
        return regexFallbackType;
    }

    public static final class Builder {

        private String key;

        private String path;

        private boolean apiIgnored;

        private MaskType annotationType = MaskType.UNKNOWN;

        private MaskType regexFallbackType = MaskType.UNKNOWN;

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
            this.annotationType = annotationType == null ? MaskType.UNKNOWN : annotationType;
            return this;
        }

        public Builder regexFallbackType(MaskType regexFallbackType) {
            this.regexFallbackType = regexFallbackType == null ? MaskType.UNKNOWN : regexFallbackType;
            return this;
        }

        public MaskRuleRequest build() {
            return new MaskRuleRequest(this);
        }
    }
}
