package com.safeoutput.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class MaskRuleMatcher {

    private final List<MaskRule> configuredRules;

    private final List<MaskRule> defaultRules;

    private MaskRuleMatcher(Collection<MaskRule> configuredRules, Collection<MaskRule> defaultRules) {
        this.configuredRules = enabledRules(configuredRules);
        this.defaultRules = enabledRules(defaultRules);
    }

    public static MaskRuleMatcher withDefaultRules() {
        return withConfiguredRules(Collections.<MaskRule>emptyList());
    }

    public static MaskRuleMatcher withConfiguredRules(Collection<MaskRule> configuredRules) {
        return new MaskRuleMatcher(configuredRules, defaultRules());
    }

    public Optional<RuleMatch> match(String key, String path) {
        Optional<RuleMatch> configuredPath = matchPath(configuredRules, path);
        if (configuredPath.isPresent()) {
            return configuredPath;
        }
        Optional<RuleMatch> configuredKey = matchKey(configuredRules, key);
        if (configuredKey.isPresent()) {
            return configuredKey;
        }
        Optional<RuleMatch> defaultPath = matchPath(defaultRules, path);
        if (defaultPath.isPresent()) {
            return defaultPath;
        }
        return matchKey(defaultRules, key);
    }

    private static List<MaskRule> defaultRules() {
        List<MaskRule> rules = new ArrayList<MaskRule>();
        rules.add(MaskRule.defaults("default.mobile")
                .keys(Arrays.asList("mobile", "phone", "telephone", "tel", "userMobile"))
                .type(MaskType.MOBILE)
                .build());
        rules.add(MaskRule.defaults("default.id-card")
                .keys(Arrays.asList("idCard", "certNo", "identityNo", "certificateNo"))
                .type(MaskType.ID_CARD)
                .build());
        rules.add(MaskRule.defaults("default.bank-card")
                .keys(Arrays.asList("bankCard", "cardNo", "bankNo"))
                .type(MaskType.BANK_CARD)
                .build());
        rules.add(MaskRule.defaults("default.email")
                .keys(Arrays.asList("email", "mail"))
                .type(MaskType.EMAIL)
                .build());
        rules.add(MaskRule.defaults("default.password")
                .keys(Arrays.asList("password", "secret", "token"))
                .type(MaskType.PASSWORD)
                .build());
        return rules;
    }

    private static List<MaskRule> enabledRules(Collection<MaskRule> rules) {
        if (rules == null) {
            return Collections.emptyList();
        }
        List<MaskRule> enabled = new ArrayList<MaskRule>();
        for (MaskRule rule : rules) {
            if (isUsable(rule)) {
                enabled.add(rule);
            }
        }
        return Collections.unmodifiableList(enabled);
    }

    private static boolean isUsable(MaskRule rule) {
        return rule != null && rule.isEnabled() && rule.getType() != null && rule.getType() != MaskType.UNKNOWN;
    }

    private static Optional<RuleMatch> matchPath(List<MaskRule> rules, String path) {
        if (path == null || path.trim().isEmpty()) {
            return Optional.empty();
        }
        for (MaskRule rule : rules) {
            for (String candidate : rule.getPaths()) {
                if (path.equals(candidate)) {
                    return Optional.of(toMatch(rule));
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<RuleMatch> matchKey(List<MaskRule> rules, String key) {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey == null) {
            return Optional.empty();
        }
        for (MaskRule rule : rules) {
            for (String candidate : rule.getKeys()) {
                if (normalizedKey.equals(normalizeKey(candidate))) {
                    return Optional.of(toMatch(rule));
                }
            }
        }
        return Optional.empty();
    }

    private static RuleMatch toMatch(MaskRule rule) {
        return new RuleMatch(rule.getType(), rule.getName(), rule.getSource());
    }

    private static String normalizeKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        return key.trim().toLowerCase(Locale.ENGLISH);
    }
}
