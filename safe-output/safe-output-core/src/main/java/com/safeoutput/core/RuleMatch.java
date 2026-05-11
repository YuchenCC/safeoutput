package com.safeoutput.core;

public final class RuleMatch {

    private final MaskType maskType;

    private final String ruleName;

    private final RuleSource source;

    public RuleMatch(MaskType maskType, String ruleName, RuleSource source) {
        this.maskType = maskType;
        this.ruleName = ruleName;
        this.source = source;
    }

    public MaskType getMaskType() {
        return maskType;
    }

    public String getRuleName() {
        return ruleName;
    }

    public RuleSource getSource() {
        return source;
    }
}
