package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    private final String allowedOrigins;

    public CorsConfig(@Value("${cors.allowed-origins:}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? "" : allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origens = origensPermitidas();
        if (origens.length == 0) {
            return;
        }
        registry.addMapping("/**")
                .allowedOrigins(origens)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("x-admin-token", "Authorization", "Content-Type");
    }

    String[] origensPermitidas() {
        List<String> origens = new ArrayList<>();
        for (String origem : allowedOrigins.split(",")) {
            String limpa = origem.trim();
            if (limpa.isEmpty()) {
                continue;
            }
            if (limpa.contains("*")) {
                log.warn("CORS_ALLOWED_ORIGINS com curinga ignorado; informe as origens exatas.");
                continue;
            }
            origens.add(limpa);
        }
        return origens.toArray(new String[0]);
    }
}
