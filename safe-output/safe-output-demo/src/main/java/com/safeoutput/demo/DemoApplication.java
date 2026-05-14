package com.safeoutput.demo;

import com.safeoutput.core.MaskContext;
import com.safeoutput.core.MaskStrategy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public MaskStrategy mobileMStrategy() {
        return new MaskStrategy() {
            @Override
            public String type() {
                return "mobileM";
            }

            @Override
            public String mask(String rawValue, MaskContext context) {
                if (rawValue == null || rawValue.length() < 7 || rawValue.contains("****")) {
                    return rawValue;
                }
                return "m-" + rawValue.substring(0, 3) + "****" + rawValue.substring(rawValue.length() - 4);
            }
        };
    }
}
