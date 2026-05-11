package com.safeoutput.spring.boot.autoconfigure;

import com.safeoutput.core.ObjectMasker;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Spring MVC response 脱敏自动装配。
 */
@Configuration
@ConditionalOnClass(ResponseBodyAdvice.class)
public class SafeOutputMvcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SafeOutputResponseBodyAdvice safeOutputResponseBodyAdvice(ObjectMasker objectMasker,
            SafeOutputProperties properties) {
        return new SafeOutputResponseBodyAdvice(objectMasker, properties);
    }
}
