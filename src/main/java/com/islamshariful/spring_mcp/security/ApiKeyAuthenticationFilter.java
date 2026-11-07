package com.islamshariful.spring_mcp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Checks the API key on every request.
 * <ul>
 *   <li><b>No key:</b> the request continues as anonymous. Public tools (weather) still work;
 *       task tools refuse to run.</li>
 *   <li><b>Valid key:</b> the request is logged in as the key's user.</li>
 *   <li><b>Wrong key:</b> rejected right away with HTTP 401, so typos are easy to spot.</li>
 * </ul>
 * The key can be sent as {@code Authorization: Bearer <key>} or {@code X-API-Key: <key>}.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    /** SHA-256 hash (hex) -> user name. */
    private final Map<String, String> usersByKeyHash;

    public ApiKeyAuthenticationFilter(ApiKeyProperties properties) {
        this.usersByKeyHash = properties.apiKeys().stream()
                .collect(Collectors.toUnmodifiableMap(key -> key.sha256().toLowerCase(), ApiKeyProperties.ApiKey::user));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String apiKey = extractApiKey(request);

        if (apiKey != null) {
            String user = findUser(apiKey);
            if (user == null) {
                log.warn("Rejected request with an invalid API key from {}", request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid API key\"}");
                return;
            }
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    user, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(request, response);
    }

    private static String extractApiKey(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).strip();
        }
        String apiKeyHeader = request.getHeader("X-API-Key");
        return (apiKeyHeader == null || apiKeyHeader.isBlank()) ? null : apiKeyHeader.strip();
    }

    /** Hashes the key and looks it up. Uses a constant-time compare to avoid timing attacks. */
    private String findUser(String apiKey) {
        byte[] hash = sha256(apiKey);
        for (var entry : usersByKeyHash.entrySet()) {
            if (MessageDigest.isEqual(hash, HexFormat.of().parseHex(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return null;
    }

    static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
