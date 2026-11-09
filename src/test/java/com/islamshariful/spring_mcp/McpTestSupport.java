package com.islamshariful.spring_mcp;

import com.islamshariful.spring_mcp.security.ApiKeyTestHashes;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.net.http.HttpRequest;
import java.time.Duration;

/** Shared helpers for end-to-end tests that talk to the real server with the MCP Java client. */
public final class McpTestSupport {

    public static final String ALICE_KEY = "test-key-alice";
    public static final String BOB_KEY = "test-key-bob";

    private McpTestSupport() {}

    /** Replaces the real API keys from application.yaml with two test users: alice and bob. */
    public static void registerTestApiKeys(DynamicPropertyRegistry registry) {
        registry.add("app.security.api-keys[0].user", () -> "alice");
        registry.add("app.security.api-keys[0].sha256", () -> ApiKeyTestHashes.sha256Hex(ALICE_KEY));
        registry.add("app.security.api-keys[1].user", () -> "bob");
        registry.add("app.security.api-keys[1].sha256", () -> ApiKeyTestHashes.sha256Hex(BOB_KEY));
    }

    /** Connects to the server, optionally with an API key, and runs the MCP handshake. */
    public static McpSyncClient connect(int port, String apiKey) {
        HttpRequest.Builder request = HttpRequest.newBuilder();
        if (apiKey != null) {
            request.header("Authorization", "Bearer " + apiKey);
        }
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                .endpoint("/mcp")
                .requestBuilder(request)
                .build();
        McpSyncClient client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
        client.initialize();
        return client;
    }
}
