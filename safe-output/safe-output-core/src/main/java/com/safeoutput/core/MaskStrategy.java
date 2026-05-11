package com.safeoutput.core;

public interface MaskStrategy {

    MaskType supportType();

    String mask(String rawValue, MaskContext context);

    default MaskResult apply(MaskContext context) {
        if (context == null) {
            return MaskResult.unchanged(MaskContext.builder().build());
        }
        String rawValue = context.getRawValue();
        if (context.getMaskType() == MaskType.UNKNOWN || context.getMaskType() != supportType()
                || rawValue == null || rawValue.length() <= 1) {
            return MaskResult.unchanged(context);
        }

        String maskedValue = mask(rawValue, context);
        if (maskedValue == null || maskedValue.equals(rawValue)) {
            return MaskResult.unchanged(context);
        }
        return MaskResult.masked(context, maskedValue);
    }
}
