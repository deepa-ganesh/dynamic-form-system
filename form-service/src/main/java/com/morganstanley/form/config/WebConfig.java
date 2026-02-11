package com.morganstanley.form.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring Web MVC configuration.
 *
 * Configures:
 * - JSON serialization/deserialization
 * - Java 8 date/time handling
 * - Pretty printing for responses
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure HTTP message converters.
     * Customize JSON serialization.
     *
     * @param converters list of converters to configure
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("Configuring JSON message converters");

        MappingJackson2HttpMessageConverter jsonConverter =
            new MappingJackson2HttpMessageConverter(objectMapper());

        converters.add(jsonConverter);
    }

    /**
     * ObjectMapper for JSON serialization.
     *
     * Configuration:
     * - Handle Java 8 date/time types (LocalDateTime, etc.)
     * - Format dates as ISO-8601 strings (not timestamps)
     * - Pretty print JSON responses (for readability)
     * - Don't fail on unknown properties
     *
     * @return configured ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        log.info("ObjectMapper configured with JavaTimeModule and ISO-8601 dates");

        return mapper;
    }
}
