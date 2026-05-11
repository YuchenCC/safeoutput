package com.safeoutput.core;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SensitiveFieldResolver {

    private final MaskRuleMatcher matcher;

    private final ConcurrentMap<Field, FieldMetadata> fieldMetadataCache =
            new ConcurrentHashMap<Field, FieldMetadata>();

    public SensitiveFieldResolver(MaskRuleMatcher matcher) {
        this.matcher = matcher == null ? MaskRuleMatcher.withDefaultRules() : matcher;
    }

    public Optional<RuleMatch> resolve(Field field, String path) {
        if (field == null) {
            return Optional.empty();
        }
        FieldMetadata metadata = fieldMetadataCache.computeIfAbsent(field, SensitiveFieldResolver::metadata);
        return matcher.decide(MaskRuleRequest.builder()
                .key(metadata.getFieldName())
                .path(path)
                .annotationType(metadata.getAnnotationType())
                .build());
    }

    int cachedFieldCount() {
        return fieldMetadataCache.size();
    }

    private static FieldMetadata metadata(Field field) {
        Desensitize desensitize = field.getAnnotation(Desensitize.class);
        MaskType annotationType = desensitize == null ? MaskType.UNKNOWN : desensitize.type();
        return new FieldMetadata(field.getName(), annotationType);
    }

    private static final class FieldMetadata {

        private final String fieldName;

        private final MaskType annotationType;

        private FieldMetadata(String fieldName, MaskType annotationType) {
            this.fieldName = fieldName;
            this.annotationType = annotationType;
        }

        private String getFieldName() {
            return fieldName;
        }

        private MaskType getAnnotationType() {
            return annotationType;
        }
    }
}
