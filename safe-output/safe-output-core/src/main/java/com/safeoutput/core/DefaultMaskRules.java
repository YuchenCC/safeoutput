package com.safeoutput.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Built-in field-name rule library.
 */
public final class DefaultMaskRules {

    private static final List<MaskRule> RULES = buildRules();

    private DefaultMaskRules() {
    }

    public static List<MaskRule> all() {
        return RULES;
    }

    private static List<MaskRule> buildRules() {
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
        return Collections.unmodifiableList(rules);
    }
}
