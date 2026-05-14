package com.safeoutput.core;

import java.util.Optional;
import java.util.logging.Logger;

public final class DefaultSafeOutputMaskService implements SafeOutputMaskService {

    private static final Logger LOGGER = Logger.getLogger(DefaultSafeOutputMaskService.class.getName());

    private final MaskStrategyRegistry strategyRegistry;

    private final ObjectMasker objectMasker;

    public DefaultSafeOutputMaskService(MaskStrategyRegistry strategyRegistry) {
        this(strategyRegistry, null);
    }

    public DefaultSafeOutputMaskService(MaskStrategyRegistry strategyRegistry, ObjectMasker objectMasker) {
        this.strategyRegistry = strategyRegistry == null ? MaskStrategyRegistry.withBuiltIns() : strategyRegistry;
        this.objectMasker = objectMasker == null
                ? new ObjectMasker(this.strategyRegistry, MaskRuleMatcher.withDefaultRules(),
                        ObjectMaskerOptions.defaults())
                : objectMasker;
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
            // 指定 type 主动脱敏只按类型标签找策略，不做字段规则匹配或 regex 扫描。
            return strategy.get().mask(value, MaskContext.builder()
                    .maskType(type)
                    .scene(MaskScene.UNKNOWN)
                    .rawValue(value)
                    .build());
        } catch (RuntimeException ex) {
            return value;
        }
    }

    @Override
    public Object maskObject(Object value) {
        try {
            // 对象主动脱敏复用响应对象递归能力，默认不对普通字符串做全局 regex 扫描。
            return objectMasker.mask(value);
        } catch (RuntimeException ex) {
            return value;
        }
    }
}
