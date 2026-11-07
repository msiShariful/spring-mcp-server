package com.islamshariful.spring_mcp.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Security setup.
 *
 * <p>The {@code /mcp} endpoint stays open so public tools (weather) work without a key.
 * Protection happens per tool: task tools ask {@code SecurityUserProvider} for the
 * logged-in user, which refuses anonymous callers.
 */
@Configuration
@EnableConfigurationProperties(ApiKeyProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ApiKeyProperties apiKeys) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/mcp", "/mcp/**").permitAll()
                        .anyRequest().denyAll())
                // Logged-in state comes from the API key on each request, not from cookies.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF protects browser cookie sessions; this API doesn't use them.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(new ApiKeyAuthenticationFilter(apiKeys), AnonymousAuthenticationFilter.class)
                .build();
    }

    /** No username/password users. This also stops Spring Boot from generating a default password. */
    @Bean
    UserDetailsService noPasswordUsers() {
        return new InMemoryUserDetailsManager();
    }
}
