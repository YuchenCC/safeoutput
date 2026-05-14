package com.safeoutput.core;

import java.util.Collection;
import java.util.Optional;
import java.util.logging.Logger;

public final class DefaultSafeOutputMaskService implements SafeOutputMaskService {

    private static final Logger LOGGER = Logger.getLogger(DefaultSafeOutputMaskService.class.getName());

    private final MaskStrategyRegistry strategyRegistry;

    private final ObjectMasker objectMasker;

    private final StrongTextMasker strongTextMasker;

    private final StrongObjectMasker strongObjectMasker;

    private final MaskEventRecorder maskEventRecorder;

    public DefaultSafeOutputMaskService(MaskStrategyRegistry strategyRegistry) {
        this(strategyRegistry, null);
    }

    public DefaultSafeOutputMaskService(MaskStrategyRegistry strategyRegistry, ObjectMasker objectMasker) {
        this(strategyRegistry, objectMasker, null);
    }

    public DefaultSafeOutputMaskService(MaskStrategyRegistry strategyRegistry, ObjectMasker objectMasker,
            Collection<String> strongFallbackTypes) {
        this(strategyRegistry, objectMasker, strongFallbackTypes, null);
    }

    public DefaultSafeOutputMaskService(MaskStrategyRegistry strategyRegistry, ObjectMasker objectMasker,
            Collection<String> strongFallbackTypes, MaskEventRecorder maskEventRecorder) {
        this.strategyRegistry = strategyRegistry == null ? MaskStrategyRegistry.withBuiltIns() : strategyRegistry;
        this.maskEventRecorder = maskEventRecorder;
        this.objectMasker = objectMasker == null
                ? new ObjectMasker(this.strategyRegistry, MaskRuleMatcher.withDefaultRules(),
                        ObjectMaskerOptions.defaults())
                : objectMasker;
        MaskRuleMatcher defaultRuleMatcher = MaskRuleMatcher.withDefaultRules();
        this.strongTextMasker = new StrongTextMasker(this.strategyRegistry, defaultRuleMatcher, strongFallbackTypes,
                maskEventRecorder);
        this.strongObjectMasker = new StrongObjectMasker(this.strongTextMasker, ObjectMaskerOptions.defaults());
    }

    @Override
    public String mask(String value, String type) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            Optional<MaskStrategy> strategy = strategyRegistry.find(type);
            if (!strategy.isPresent()) {
                LOGGER.warning("Skip manual masking because no strategy registered for type "
                        + MaskTypes.normalize(type));
                return value;
            }
            long startedAt = System.nanoTime();
            // 指定 type 主动脱敏只按类型标签找策略，不做字段规则匹配或 regex 扫描。
            String masked = strategy.get().mask(value, MaskContext.builder()
                    .maskType(type)
                    .scene(MaskScene.MANUAL)
                    .rawValue(value)
                    .build());
            if (!value.equals(masked)) {
                recordManualMask(type, System.nanoTime() - startedAt);
            }
            return masked;
        } catch (RuntimeException ex) {
            return value;
        }
    }

    @Override
    public Object maskObject(Object value) {
        try {
            // 对象主动脱敏复用响应对象递归能力，默认不对普通字符串做全局 regex 扫描。
            return objectMasker.mask(value, MaskScene.MANUAL);
        } catch (RuntimeException ex) {
            return value;
        }
    }

    @Override
    public String maskStrong(String value) {
        return strongTextMasker.mask(value);
    }

    @Override
    public Object maskObjectStrong(Object value) {
        try {
            // 强扫描必须由调用方显式进入；该路径会扫描对象中的普通字符串。
            return strongObjectMasker.mask(value);
        } catch (RuntimeException ex) {
            return value;
        }
    }

    private void recordManualMask(String type, long elapsedNanos) {
        if (maskEventRecorder != null) {
            maskEventRecorder.recordMask(MaskScene.MANUAL, type, elapsedNanos);
        }
    }
}
