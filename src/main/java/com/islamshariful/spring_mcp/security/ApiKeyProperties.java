package com.islamshariful.spring_mcp.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * API keys loaded from {@code app.security.api-keys} in application.yaml.
 * Each entry links a user to the SHA-256 hash of their key (never the key itself).
 */
@ConfigurationProperties(prefix = "app.security")
public record ApiKeyProperties(List<ApiKey> apiKeys) {

    public ApiKeyProperties {
        apiKeys = (apiKeys == null) ? List.of() : List.copyOf(apiKeys);
    }

    public record ApiKey(String user, String sha256) {}
}
