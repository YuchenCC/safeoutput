package com.safeoutput.spring.boot.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SpringFactoriesTest {

    private static final String AUTO_CONFIGURATION =
            "com.safeoutput.spring.boot.autoconfigure.SafeOutputAutoConfiguration";
    private static final String MVC_AUTO_CONFIGURATION =
            "com.safeoutput.spring.boot.autoconfigure.SafeOutputMvcAutoConfiguration";

    @Test
    void springFactoriesRegistersBoot2AutoConfigurationClass() throws Exception {
        InputStream input = getClass().getClassLoader()
                .getResourceAsStream("META-INF/spring.factories");

        assertNotNull(input);
        Properties properties = new Properties();
        properties.load(input);

        Set<String> autoConfigurations = new HashSet<String>(Arrays.asList(properties.getProperty(
                "org.springframework.boot.autoconfigure.EnableAutoConfiguration").split(",")));
        assertTrue(autoConfigurations.contains(AUTO_CONFIGURATION));
        assertTrue(autoConfigurations.contains(MVC_AUTO_CONFIGURATION));
        assertEquals(AUTO_CONFIGURATION, Class.forName(AUTO_CONFIGURATION).getName());
        assertEquals(MVC_AUTO_CONFIGURATION, Class.forName(MVC_AUTO_CONFIGURATION).getName());
    }
}
