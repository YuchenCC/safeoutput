package com.safeoutput.core;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class MaskStrategyRegistry {

    private static final Logger LOGGER = Logger.getLogger(MaskStrategyRegistry.class.getName());

    private final Map<MaskType, MaskStrategy> strategies;

    public MaskStrategyRegistry(Collection<MaskStrategy> strategies) {
        Map<MaskType, MaskStrategy> registered = new EnumMap<MaskType, MaskStrategy>(MaskType.class);
        registerAll(registered, strategies);
        this.strategies = Collections.unmodifiableMap(registered);
    }

    public static MaskStrategyRegistry withBuiltIns() {
        return withBuiltIns(Collections.<MaskStrategy>emptyList());
    }

    public static MaskStrategyRegistry withBuiltIns(Collection<MaskStrategy> customStrategies) {
        Map<MaskType, MaskStrategy> registered = new EnumMap<MaskType, MaskStrategy>(MaskType.class);
        registerAll(registered, BuiltInMaskStrategies.strategies());
        registerAll(registered, customStrategies);
        return new MaskStrategyRegistry(registered.values());
    }

    public Optional<MaskStrategy> find(MaskType type) {
        if (type == null || type == MaskType.UNKNOWN) {
            return Optional.empty();
        }
        return Optional.ofNullable(strategies.get(type));
    }

    private static void registerAll(Map<MaskType, MaskStrategy> registered, Collection<MaskStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (MaskStrategy strategy : strategies) {
            register(registered, strategy);
        }
    }

    private static void register(Map<MaskType, MaskStrategy> registered, MaskStrategy strategy) {
        if (strategy == null || strategy.supportType() == null || strategy.supportType() == MaskType.UNKNOWN) {
            return;
        }
        MaskStrategy previous = registered.put(strategy.supportType(), strategy);
        if (previous != null && previous != strategy) {
            LOGGER.info("Override mask strategy for type " + strategy.supportType().getCode()
                    + " with " + strategy.getClass().getName()
                    + ", previous " + previous.getClass().getName());
        }
    }
}
