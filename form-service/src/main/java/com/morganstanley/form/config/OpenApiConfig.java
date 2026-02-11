package com.morganstanley.form.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration.
 *
 * Provides interactive API documentation at:
 * http://localhost:8080/api/swagger-ui.html
 *
 * API docs JSON at:
 * http://localhost:8080/api/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configure OpenAPI documentation.
     *
     * @return OpenAPI configuration
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Dynamic Versioned Form Management System API")
                .version("1.0.0")
                .description("""
                    REST API for managing dynamic forms with automatic versioning and schema evolution.

                    **Key Features:**
                    - Automatic version control (immutable versions)
                    - WIP (draft) and COMMITTED (final) versions
                    - Dynamic schema evolution without data migration
                    - Complete audit trail
                    - Backward compatibility

                    **Authentication:**
                    - Demo users: `user/password` (USER role), `admin/admin` (ADMIN role)
                    - Use HTTP Basic authentication
                    - Header format: `Authorization: Basic <base64(username:password)>`

                    **Base Path:** `/api`

                    **Typical Workflow:**
                    1. Create WIP version: `POST /v1/orders` with `finalSave: false`
                    2. Auto-save drafts (creates new WIP versions)
                    3. Final submit: `POST /v1/orders` with `finalSave: true` (creates COMMITTED version)
                    4. View history: `GET /v1/orders/{orderId}/versions`
                    """)
                .contact(new Contact()
                    .name("Deepa Ganesh")
                    .email("deepa.ganesh@morganstanley.com")
                )
                .license(new License()
                    .name("Proprietary")
                    .url("https://www.morganstanley.com")
                )
            )
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080/api")
                    .description("Local development server"),
                new Server()
                    .url("https://api.example.com")
                    .description("Production server (example)")
            ))
            .components(new Components()
                .addSecuritySchemes("basicAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("basic")
                    .description("HTTP Basic Authentication. Use `user/password` or `admin/admin` for demo.")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}
