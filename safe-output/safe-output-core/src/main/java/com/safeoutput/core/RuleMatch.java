package com.safeoutput.core;

public final class RuleMatch {

    private final MaskType maskType;

    private final String ruleName;

    private final RuleSource source;

    private final RuleAction action;

    public RuleMatch(MaskType maskType, String ruleName, RuleSource source) {
        this(maskType, ruleName, source, RuleAction.MASK);
    }

    public RuleMatch(MaskType maskType, String ruleName, RuleSource source, RuleAction action) {
        this.maskType = maskType;
        this.ruleName = ruleName;
        this.source = source;
        this.action = action;
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

    public RuleAction getAction() {
        return action;
    }
}
