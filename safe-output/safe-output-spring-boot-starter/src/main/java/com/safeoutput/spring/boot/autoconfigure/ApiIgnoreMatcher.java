package com.safeoutput.spring.boot.autoconfigure;

import com.safeoutput.core.MaskScene;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.util.AntPathMatcher;

final class ApiIgnoreMatcher {

    private final SafeOutputProperties.IgnoreProperties ignore;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    ApiIgnoreMatcher(SafeOutputProperties.IgnoreProperties ignore) {
        this.ignore = ignore;
    }

    Optional<ApiIgnoreMatch> match(String method, String path, MaskScene scene) {
        if (ignore == null || method == null || path == null) {
            return Optional.empty();
        }
        for (SafeOutputProperties.ApiIgnoreProperties api : ignore.getApis()) {
            if (matches(api, method, path, scene)) {
                return Optional.of(new ApiIgnoreMatch(api.getReason()));
            }
        }
        return Optional.empty();
    }

    private boolean matches(SafeOutputProperties.ApiIgnoreProperties api, String method, String path,
            MaskScene scene) {
        String pattern = pattern(api);
        if (pattern == null || pattern.trim().isEmpty()) {
            // Ignore 是显式豁免；缺少路径的配置不能扩大成“所有接口豁免”。
            return false;
        }
        return matchesMethod(api.getMethod(), method)
                && matchesScene(api.getScenes(), scene)
                && pathMatcher.match(pattern, path);
    }

    private static boolean matchesMethod(String expected, String actual) {
        return expected == null || expected.trim().isEmpty()
                || expected.trim().toUpperCase(Locale.ENGLISH).equals(actual.trim().toUpperCase(Locale.ENGLISH));
    }

    private static boolean matchesScene(List<MaskScene> scenes, MaskScene scene) {
        return scenes == null || scenes.isEmpty() ? scene == MaskScene.RESPONSE : scenes.contains(scene);
    }

    private static String pattern(SafeOutputProperties.ApiIgnoreProperties api) {
        if (api.getPath() != null && !api.getPath().trim().isEmpty()) {
            return api.getPath();
        }
        return api.getPattern();
    }
}
