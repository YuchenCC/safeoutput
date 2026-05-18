package com.safeoutput.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class MaskRuleMatcher {

    private final List<MaskRule> configuredRules;

    private final List<MaskRule> defaultRules;

    private final List<String> ignoreKeys;

    private final List<String> ignorePaths;

    private MaskRuleMatcher(Builder builder) {
        this.configuredRules = enabledRules(builder.configuredRules);
        this.defaultRules = builder.defaultRulesEnabled
                ? enabledRules(defaultRules())
                : Collections.<MaskRule>emptyList();
        this.ignoreKeys = normalizedKeys(builder.ignoreKeys);
        this.ignorePaths = immutableStrings(builder.ignorePaths);
    }

    private MaskRuleMatcher(Collection<MaskRule> configuredRules, Collection<MaskRule> defaultRules) {
        this.configuredRules = enabledRules(configuredRules);
        this.defaultRules = enabledRules(defaultRules);
        this.ignoreKeys = Collections.emptyList();
        this.ignorePaths = Collections.emptyList();
    }

    public static MaskRuleMatcher withDefaultRules() {
        return withConfiguredRules(Collections.<MaskRule>emptyList());
    }

    public static MaskRuleMatcher withConfiguredRules(Collection<MaskRule> configuredRules) {
        return new MaskRuleMatcher(configuredRules, defaultRules());
    }

    public static Builder builder() {
        return new Builder();
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

    public Optional<RuleMatch> decide(MaskRuleRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        // 这里固定 Safe Output 的规则优先级：Ignore 优先于注解，注解优先于 Rule，fallback 最后执行。
        if (request.isApiIgnored()) {
            return Optional.of(ignore("api-ignore", RuleSource.API_IGNORE));
        }
        Optional<RuleMatch> fieldIgnore = matchFieldIgnore(request.getKey(), request.getPath());
        if (fieldIgnore.isPresent()) {
            return fieldIgnore;
        }
        if (isMaskType(request.getAnnotationType())) {
            return Optional.of(new RuleMatch(request.getAnnotationType(), "annotation", RuleSource.ANNOTATION));
        }
        Optional<RuleMatch> ruleMatch = match(request.getKey(), request.getPath());
        if (ruleMatch.isPresent()) {
            return ruleMatch;
        }
        if (isMaskType(request.getRegexFallbackType())) {
            return Optional.of(new RuleMatch(request.getRegexFallbackType(), "regex-fallback",
                    RuleSource.REGEX_FALLBACK));
        }
        return Optional.empty();
    }

    public Map<String, RuleMatch> logKeyMatches(int maxKeys) {
        if (maxKeys <= 0) {
            return Collections.emptyMap();
        }
        Map<String, RuleMatch> matches = new LinkedHashMap<String, RuleMatch>();
        if (!appendLogRuleKeys(matches, configuredRules, maxKeys)
                || !appendLogRuleKeys(matches, defaultRules, maxKeys)) {
            return Collections.emptyMap();
        }
        for (String ignoredKey : ignoreKeys) {
            matches.put(ignoredKey, ignore("field-ignore-key", RuleSource.FIELD_IGNORE));
        }
        return Collections.unmodifiableMap(matches);
    }

    private Optional<RuleMatch> matchFieldIgnore(String key, String path) {
        String normalizedKey = normalizeKey(key);
        if (normalizedKey != null && ignoreKeys.contains(normalizedKey)) {
            return Optional.of(ignore("field-ignore-key", RuleSource.FIELD_IGNORE));
        }
        if (matchesPath(ignorePaths, path)) {
            return Optional.of(ignore("field-ignore-path", RuleSource.FIELD_IGNORE));
        }
        return Optional.empty();
    }

    private static List<MaskRule> defaultRules() {
        List<MaskRule> rules = new ArrayList<MaskRule>();
        // 默认规则只覆盖语义清晰的字段名；name/id/code/no 等歧义字段必须由显式 Rule 或注解声明。
        rules.add(MaskRule.defaults("default.mobile")
                .keys(Arrays.asList("mobile", "phone", "telephone", "tel", "userMobile"))
                .type(MaskTypes.MOBILE)
                .build());
        rules.add(MaskRule.defaults("default.id-card")
                .keys(Arrays.asList("idCard", "certNo", "identityNo", "certificateNo"))
                .type(MaskTypes.ID_CARD)
                .build());
        rules.add(MaskRule.defaults("default.bank-card")
                .keys(Arrays.asList("bankCard", "cardNo", "bankNo"))
                .type(MaskTypes.BANK_CARD)
                .build());
        rules.add(MaskRule.defaults("default.email")
                .keys(Arrays.asList("email", "mail"))
                .type(MaskTypes.EMAIL)
                .build());
        rules.add(MaskRule.defaults("default.password")
                .keys(Arrays.asList("password", "secret", "token"))
                .type(MaskTypes.PASSWORD)
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
        return rule != null && rule.isEnabled() && !MaskTypes.isUnknown(rule.getType());
    }

    private static Optional<RuleMatch> matchPath(List<MaskRule> rules, String path) {
        if (path == null || path.trim().isEmpty()) {
            return Optional.empty();
        }
        for (MaskRule rule : rules) {
            for (String candidate : rule.getPaths()) {
                if (pathMatches(candidate, path)) {
                    return Optional.of(toMatch(rule));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean matchesPath(Collection<String> candidates, String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        for (String candidate : candidates) {
            if (pathMatches(candidate, path)) {
                return true;
            }
        }
        return false;
    }

    private static boolean pathMatches(String candidate, String path) {
        if (candidate == null) {
            return false;
        }
        if (path.equals(candidate)) {
            return true;
        }
        if (candidate.indexOf("[*]") < 0) {
            return false;
        }
        return wildcardPathMatches(candidate, path);
    }

    private static boolean wildcardPathMatches(String candidate, String path) {
        int candidateIndex = 0;
        int pathIndex = 0;
        while (candidateIndex < candidate.length()) {
            int wildcardIndex = candidate.indexOf("[*]", candidateIndex);
            if (wildcardIndex < 0) {
                return path.regionMatches(pathIndex, candidate, candidateIndex,
                        candidate.length() - candidateIndex) && pathIndex + candidate.length() - candidateIndex
                        == path.length();
            }
            if (!path.regionMatches(pathIndex, candidate, candidateIndex, wildcardIndex - candidateIndex)) {
                return false;
            }
            pathIndex += wildcardIndex - candidateIndex;
            if (pathIndex >= path.length() || path.charAt(pathIndex) != '[') {
                return false;
            }
            pathIndex++;
            int digitStart = pathIndex;
            while (pathIndex < path.length() && Character.isDigit(path.charAt(pathIndex))) {
                pathIndex++;
            }
            if (pathIndex == digitStart || pathIndex >= path.length() || path.charAt(pathIndex) != ']') {
                return false;
            }
            pathIndex++;
            candidateIndex = wildcardIndex + 3;
        }
        return pathIndex == path.length();
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

    private static boolean appendLogRuleKeys(Map<String, RuleMatch> matches, List<MaskRule> rules, int maxKeys) {
        for (MaskRule rule : rules) {
            for (String candidate : rule.getKeys()) {
                String normalizedKey = normalizeKey(candidate);
                if (normalizedKey != null && !matches.containsKey(normalizedKey)) {
                    if (matches.size() >= maxKeys) {
                        return false;
                    }
                    matches.put(normalizedKey, toMatch(rule));
                }
            }
        }
        return true;
    }

    private static RuleMatch ignore(String ruleName, RuleSource source) {
        return new RuleMatch(MaskTypes.UNKNOWN, ruleName, source, RuleAction.IGNORE);
    }

    private static boolean isMaskType(String type) {
        return !MaskTypes.isUnknown(type);
    }

    private static List<String> normalizedKeys(Collection<String> keys) {
        if (keys == null) {
            return Collections.emptyList();
        }
        List<String> normalized = new ArrayList<String>();
        for (String key : keys) {
            String normalizedKey = normalizeKey(key);
            if (normalizedKey != null) {
                normalized.add(normalizedKey);
            }
        }
        return Collections.unmodifiableList(normalized);
    }

    private static List<String> immutableStrings(Collection<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<String>(values));
    }

    private static String normalizeKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        return key.trim().toLowerCase(Locale.ENGLISH);
    }

    public static final class Builder {

        private final List<MaskRule> configuredRules = new ArrayList<MaskRule>();

        private final List<String> ignoreKeys = new ArrayList<String>();

        private final List<String> ignorePaths = new ArrayList<String>();

        private boolean defaultRulesEnabled = true;

        private Builder() {
        }

        public Builder configuredRules(Collection<MaskRule> rules) {
            if (rules != null) {
                this.configuredRules.addAll(rules);
            }
            return this;
        }

        public Builder ignoreKeys(Collection<String> keys) {
            if (keys != null) {
                this.ignoreKeys.addAll(keys);
            }
            return this;
        }

        public Builder ignorePaths(Collection<String> paths) {
            if (paths != null) {
                this.ignorePaths.addAll(paths);
            }
            return this;
        }

        public Builder defaultRulesEnabled(boolean defaultRulesEnabled) {
            this.defaultRulesEnabled = defaultRulesEnabled;
            return this;
        }

        public MaskRuleMatcher build() {
            return new MaskRuleMatcher(this);
        }
    }
}
