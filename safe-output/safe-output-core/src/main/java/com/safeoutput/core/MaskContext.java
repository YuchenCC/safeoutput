package com.safeoutput.core;

public final class MaskContext {

    private final MaskType maskType;
    private final MaskScene scene;
    private final String path;
    private final String fieldName;
    private final String rawValue;

    private MaskContext(Builder builder) {
        this.maskType = builder.maskType == null ? MaskType.UNKNOWN : builder.maskType;
        this.scene = builder.scene == null ? MaskScene.UNKNOWN : builder.scene;
        this.path = builder.path;
        this.fieldName = builder.fieldName;
        this.rawValue = builder.rawValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public MaskType getMaskType() {
        return maskType;
    }

    public MaskScene getScene() {
        return scene;
    }

    public String getPath() {
        return path;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getRawValue() {
        return rawValue;
    }

    public static final class Builder {

        private MaskType maskType;
        private MaskScene scene;
        private String path;
        private String fieldName;
        private String rawValue;

        private Builder() {
        }

        public Builder maskType(MaskType value) {
            this.maskType = value;
            return this;
        }

        public Builder scene(MaskScene value) {
            this.scene = value;
            return this;
        }

        public Builder path(String value) {
            this.path = value;
            return this;
        }

        public Builder fieldName(String value) {
            this.fieldName = value;
            return this;
        }

        public Builder rawValue(String value) {
            this.rawValue = value;
            return this;
        }

        public MaskContext build() {
            return new MaskContext(this);
        }
    }
}
