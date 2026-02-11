package com.morganstanley.form.config;

import lombok.extern.slf4j.Slf4j;
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
 * For demo: Allow all origins.
 * For production: Restrict to specific frontend domains.
 */
@Configuration
@Slf4j
public class CorsConfig {

    /**
     * Configure CORS filter.
     *
     * @return CorsFilter
     */
    @Bean
    public CorsFilter corsFilter() {
        log.info("Configuring CORS filter");

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("CORS configured: allowing all origins (DEMO MODE)");

        return new CorsFilter(source);
    }
}
