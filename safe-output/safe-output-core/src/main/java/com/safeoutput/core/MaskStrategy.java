package com.safeoutput.core;

public interface MaskStrategy {

    String type();

    default MaskType supportType() {
        return MaskType.fromCode(type());
    }

    String mask(String rawValue, MaskContext context);

    default MaskResult apply(MaskContext context) {
        if (context == null) {
            return MaskResult.unchanged(MaskContext.builder().build());
        }
        String rawValue = context.getRawValue();
        if (MaskTypes.isUnknown(context.getMaskType()) || !MaskTypes.same(context.getMaskType(), type())
                || rawValue == null || rawValue.isEmpty()) {
            return MaskResult.unchanged(context);
        }

        String maskedValue = mask(rawValue, context);
        if (maskedValue == null || maskedValue.equals(rawValue)) {
            return MaskResult.unchanged(context);
        }
        return MaskResult.masked(context, maskedValue);
    }
}
