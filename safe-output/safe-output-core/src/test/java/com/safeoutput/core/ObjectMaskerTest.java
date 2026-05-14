package com.safeoutput.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ObjectMaskerTest {

    @Test
    void masksBeanMapListArrayAndNestedObjects() {
        ObjectMasker masker = defaultMasker();
        CustomerPayload payload = new CustomerPayload();
        payload.mobile = "13812345678";
        payload.idCard = "110105199902300029";
        payload.profile = new ProfilePayload();
        payload.profile.email = "alice@example.com";
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("password", "secret-value");
        map.put("items", Arrays.asList(bean("13912345678"), bean("13712345678")));
        payload.attributes = map;
        payload.contacts = new ContactPayload[] {bean("13612345678")};

        CustomerPayload masked = (CustomerPayload) masker.mask(payload);

        assertSame(payload, masked);
        assertEquals("138****5678", masked.mobile);
        assertEquals("110105********0029", masked.idCard);
        assertEquals("ali****@example.com", masked.profile.email);
        assertEquals("********", masked.attributes.get("password"));
        assertEquals("139****5678", ((ContactPayload) ((List<?>) masked.attributes.get("items")).get(0)).mobile);
        assertEquals("136****5678", masked.contacts[0].mobile);
    }

    @Test
    void honorsDepthCycleProtectionAndUnsupportedTypes() {
        ObjectMasker masker = new ObjectMasker(MaskStrategyRegistry.withBuiltIns(),
                MaskRuleMatcher.withDefaultRules(), ObjectMaskerOptions.builder()
                        .maxDepth(1)
                        .build());
        CustomerPayload payload = new CustomerPayload();
        payload.mobile = "13812345678";
        payload.profile = new ProfilePayload();
        payload.profile.email = "alice@example.com";
        payload.attachments = Arrays.asList(new ByteArrayInputStream(new byte[] {1}));
        payload.contacts = new ContactPayload[] {bean("13912345678"), bean("13712345678")};
        payload.self = payload;

        CustomerPayload masked = (CustomerPayload) masker.mask(payload);

        assertEquals("138****5678", masked.mobile);
        assertEquals("alice@example.com", masked.profile.email);
        assertEquals("13912345678", masked.contacts[0].mobile);
        assertEquals("13712345678", masked.contacts[1].mobile);
        assertSame(payload, masked.self);
        assertSame(payload.attachments.get(0), masked.attachments.get(0));
    }

    @Test
    void honorsCollectionLimit() {
        ObjectMasker masker = new ObjectMasker(MaskStrategyRegistry.withBuiltIns(),
                MaskRuleMatcher.withDefaultRules(), ObjectMaskerOptions.builder()
                        .maxCollectionSize(1)
                        .build());
        CustomerPayload payload = new CustomerPayload();
        payload.contacts = new ContactPayload[] {bean("13912345678"), bean("13712345678")};

        CustomerPayload masked = (CustomerPayload) masker.mask(payload);

        assertEquals("139****5678", masked.contacts[0].mobile);
        assertEquals("13712345678", masked.contacts[1].mobile);
    }

    @Test
    void skipsBareSimpleValuesAndFailsOpenWhenStrategyThrows() {
        MaskStrategy throwing = new MaskStrategy() {
            @Override
            public MaskType supportType() {
                return MaskType.MOBILE;
            }

            @Override
            public String mask(String rawValue, MaskContext context) {
                throw new IllegalStateException("boom");
            }
        };
        ObjectMasker masker = new ObjectMasker(new MaskStrategyRegistry(Arrays.asList(throwing)),
                MaskRuleMatcher.withDefaultRules(), ObjectMaskerOptions.defaults());
        ContactPayload contact = bean("13812345678");

        assertEquals("13812345678", masker.mask("13812345678"));
        assertArrayEquals(new byte[] {1, 2}, (byte[]) masker.mask(new byte[] {1, 2}));
        assertSame(contact, masker.mask(contact));
        assertEquals("13812345678", contact.mobile);
    }

    private static ObjectMasker defaultMasker() {
        return new ObjectMasker(MaskStrategyRegistry.withBuiltIns(), MaskRuleMatcher.withDefaultRules(),
                ObjectMaskerOptions.defaults());
    }

    private static ContactPayload bean(String mobile) {
        ContactPayload payload = new ContactPayload();
        payload.mobile = mobile;
        return payload;
    }

    private static final class CustomerPayload {

        private String mobile;

        private String idCard;

        private ProfilePayload profile;

        private Map<String, Object> attributes;

        private ContactPayload[] contacts;

        private List<Object> attachments = new ArrayList<Object>();

        private CustomerPayload self;
    }

    private static final class ProfilePayload {

        private String email;
    }

    private static final class ContactPayload {

        private String mobile;
    }
}
