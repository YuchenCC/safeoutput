package com.safeoutput.spring.boot.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class SpringFactoriesTest {

    private static final String AUTO_CONFIGURATION =
            "com.safeoutput.spring.boot.autoconfigure.SafeOutputAutoConfiguration";

    @Test
    void springFactoriesRegistersBoot2AutoConfigurationClass() throws Exception {
        InputStream input = getClass().getClassLoader()
                .getResourceAsStream("META-INF/spring.factories");

        assertNotNull(input);
        Properties properties = new Properties();
        properties.load(input);

        assertEquals(AUTO_CONFIGURATION, properties.getProperty(
                "org.springframework.boot.autoconfigure.EnableAutoConfiguration"));
        assertEquals(AUTO_CONFIGURATION, Class.forName(AUTO_CONFIGURATION).getName());
    }
}
