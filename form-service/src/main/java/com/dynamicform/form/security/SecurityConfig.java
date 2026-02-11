package com.dynamicform.form.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration.
 *
 * For this demo/interview, using simplified in-memory authentication.
 * In production, this would integrate with:
 * - JWT tokens
 * - OAuth2/OIDC
 * - Corporate SSO (e.g., Active Directory)
 * - Database-backed user store
 *
 * Security features:
 * - HTTP Basic authentication (for demo)
 * - Role-based access control (USER, ADMIN)
 * - Method-level security (@PreAuthorize)
 * - Stateless sessions
 * - CSRF disabled (for REST API)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {

    /**
     * Configure security filter chain.
     *
     * Defines:
     * - Which endpoints require authentication
     * - Which endpoints are public
     * - Authentication method (HTTP Basic for demo)
     * - Session management (stateless)
     *
     * @param http HttpSecurity builder
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring security filter chain");

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/orders/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/v1/orders/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/v1/schemas/active").authenticated()
                .requestMatchers(HttpMethod.GET, "/v1/schemas/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/v1/schemas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/schemas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/v1/schemas/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {
            })
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

    /**
     * In-memory user details service for demo.
     *
     * Creates two users:
     * - user / password (ROLE_USER)
     * - admin / admin (ROLE_USER, ROLE_ADMIN)
     *
     * In production, replace with:
     * - JdbcUserDetailsManager (database users)
     * - LdapUserDetailsManager (LDAP/AD)
     * - Custom UserDetailsService (custom authentication)
     *
     * @return UserDetailsService with demo users
     */
    @Bean
    public UserDetailsService userDetailsService() {
        log.info("Configuring in-memory user details service (DEMO ONLY)");

        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();

        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin"))
            .roles("USER", "ADMIN")
            .build();

        UserDetails demoAdmin = User.builder()
            .username("admin@example.com")
            .password(passwordEncoder().encode("demo123"))
            .roles("USER", "ADMIN")
            .build();

        log.info("Created demo users: user (USER), admin (ADMIN), admin@example.com (ADMIN)");

        return new InMemoryUserDetailsManager(user, admin, demoAdmin);
    }

    /**
     * Password encoder bean.
     * Uses BCrypt hashing algorithm.
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
