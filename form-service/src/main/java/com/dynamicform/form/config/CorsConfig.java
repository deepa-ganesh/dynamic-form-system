package com.dynamicform.form.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) configuration.
 *
 * Allows frontend applications running on different domains/ports
 * to access this REST API.
 *
 * Allowed origins are controlled by `app.security.cors.allowed-origin-patterns`.
 * Defaults are local UI origins and should be overridden per environment.
 */
@Configuration
@Slf4j
public class CorsConfig {

    @Value("${app.security.cors.allowed-origin-patterns:http://localhost:5173,http://127.0.0.1:5173}")
    private List<String> allowedOriginPatterns;

    /**
     * Configure CORS filter.
     *
     * @return CorsFilter
     */
    @Bean
    public CorsFilter corsFilter() {
        log.info("Configuring CORS filter");

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(allowedOriginPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(!allowedOriginPatterns.contains("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("CORS configured with allowed origins: {}", allowedOriginPatterns);

        return new CorsFilter(source);
    }
}
