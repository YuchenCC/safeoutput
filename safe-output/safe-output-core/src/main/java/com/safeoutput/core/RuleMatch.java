package com.safeoutput.core;

public final class RuleMatch {

    private final String maskType;

    private final String ruleName;

    private final RuleSource source;

    private final RuleAction action;

    public RuleMatch(MaskType maskType, String ruleName, RuleSource source) {
        this(MaskTypes.from(maskType), ruleName, source, RuleAction.MASK);
    }

    public RuleMatch(String maskType, String ruleName, RuleSource source) {
        this(maskType, ruleName, source, RuleAction.MASK);
    }

    public RuleMatch(MaskType maskType, String ruleName, RuleSource source, RuleAction action) {
        this(MaskTypes.from(maskType), ruleName, source, action);
    }

    public RuleMatch(String maskType, String ruleName, RuleSource source, RuleAction action) {
        this.maskType = MaskTypes.normalize(maskType);
        this.ruleName = ruleName;
        this.source = source;
        this.action = action;
    }

    public String getMaskType() {
        return maskType;
    }

    public String getRuleName() {
        return ruleName;
    }

    public RuleSource getSource() {
        return source;
    }

    public RuleAction getAction() {
        return action;
    }
}
