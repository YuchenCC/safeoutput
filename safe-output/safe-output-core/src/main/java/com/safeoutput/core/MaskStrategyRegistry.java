package com.safeoutput.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class MaskStrategyRegistry {

    private static final Logger LOGGER = Logger.getLogger(MaskStrategyRegistry.class.getName());

    private final Map<String, MaskStrategy> strategies;

    public MaskStrategyRegistry(Collection<MaskStrategy> strategies) {
        Map<String, MaskStrategy> registered = new LinkedHashMap<String, MaskStrategy>();
        registerAll(registered, strategies);
        this.strategies = Collections.unmodifiableMap(registered);
    }

    public static MaskStrategyRegistry withBuiltIns() {
        return withBuiltIns(Collections.<MaskStrategy>emptyList());
    }

    public static MaskStrategyRegistry withBuiltIns(Collection<MaskStrategy> customStrategies) {
        Map<String, MaskStrategy> registered = new LinkedHashMap<String, MaskStrategy>();
        registerAll(registered, BuiltInMaskStrategies.strategies());
        registerAll(registered, customStrategies);
        return new MaskStrategyRegistry(registered.values());
    }

    public Optional<MaskStrategy> find(MaskType type) {
        return find(MaskTypes.from(type));
    }

    public Optional<MaskStrategy> find(String type) {
        if (MaskTypes.isUnknown(type)) {
            return Optional.empty();
        }
        return Optional.ofNullable(strategies.get(MaskTypes.normalize(type)));
    }

    private static void registerAll(Map<String, MaskStrategy> registered, Collection<MaskStrategy> strategies) {
        if (strategies == null) {
            return;
        }
        for (MaskStrategy strategy : strategies) {
            register(registered, strategy);
        }
    }

    private static void register(Map<String, MaskStrategy> registered, MaskStrategy strategy) {
        if (strategy == null || MaskTypes.isUnknown(strategy.type())) {
            return;
        }
        String type = MaskTypes.normalize(strategy.type());
        MaskStrategy previous = registered.put(type, strategy);
        if (previous != null && previous != strategy) {
            LOGGER.info("Override mask strategy for type " + type
                    + " with " + strategy.getClass().getName()
                    + ", previous " + previous.getClass().getName());
        }
    }
}
