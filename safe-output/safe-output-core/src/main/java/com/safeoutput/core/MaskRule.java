package com.safeoutput.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class MaskRule {

    private final String name;

    private final List<String> keys;

    private final List<String> paths;

    private final String type;

    private final boolean enabled;

    private final RuleSource source;

    private MaskRule(Builder builder) {
        this.name = builder.name;
        this.keys = Collections.unmodifiableList(new ArrayList<String>(builder.keys));
        this.paths = Collections.unmodifiableList(new ArrayList<String>(builder.paths));
        this.type = builder.type;
        this.enabled = builder.enabled;
        this.source = builder.source;
    }

    public static Builder configured(String name) {
        return new Builder(name, RuleSource.CONFIGURED);
    }

    public static Builder defaults(String name) {
        return new Builder(name, RuleSource.DEFAULT);
    }

    public String getName() {
        return name;
    }

    public List<String> getKeys() {
        return keys;
    }

    public List<String> getPaths() {
        return paths;
    }

    public String getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public RuleSource getSource() {
        return source;
    }

    public static final class Builder {

        private final String name;

        private final RuleSource source;

        private final List<String> keys = new ArrayList<String>();

        private final List<String> paths = new ArrayList<String>();

        private String type = MaskTypes.UNKNOWN;

        private boolean enabled = true;

        private Builder(String name, RuleSource source) {
            this.name = name;
            this.source = source;
        }

        public Builder keys(Collection<String> keys) {
            if (keys != null) {
                this.keys.addAll(keys);
            }
            return this;
        }

        public Builder paths(Collection<String> paths) {
            if (paths != null) {
                this.paths.addAll(paths);
            }
            return this;
        }

        public Builder type(MaskType type) {
            this.type = MaskTypes.from(type);
            return this;
        }

        public Builder type(String type) {
            this.type = MaskTypes.normalize(type);
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public MaskRule build() {
            return new MaskRule(this);
        }
    }
}
