package com.islamshariful.spring_mcp.security;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end test: starts the real server and talks to it with the official MCP Java client,
 * exactly like Claude or the MCP Inspector would.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:auth-test")
class McpAuthenticationTest {

    private static final String ALICE_KEY = "test-key-alice";
    private static final String BOB_KEY = "test-key-bob";

    /** Replace the real keys from application.yaml with test keys. */
    @DynamicPropertySource
    static void apiKeys(DynamicPropertyRegistry registry) {
        registry.add("app.security.api-keys[0].user", () -> "alice");
        registry.add("app.security.api-keys[0].sha256", () -> hash(ALICE_KEY));
        registry.add("app.security.api-keys[1].user", () -> "bob");
        registry.add("app.security.api-keys[1].sha256", () -> hash(BOB_KEY));
    }

    @LocalServerPort
    private int port;

    @Test
    void taskToolsRequireAnApiKey() {
        try (McpSyncClient anonymous = connect(null)) {
            CallToolResult result = call(anonymous, "list_tasks", Map.of());

            assertThat(result.isError()).isTrue();
            assertThat(text(result)).startsWith("Authentication required");
        }
    }

    @Test
    void eachUserOnlySeesTheirOwnTasks() {
        try (McpSyncClient alice = connect(ALICE_KEY); McpSyncClient bob = connect(BOB_KEY)) {
            CallToolResult created = call(alice, "create_task", Map.of("title", "Alice's secret task"));
            assertThat(created.isError()).isFalse();

            assertThat(text(call(alice, "list_tasks", Map.of()))).contains("Alice's secret task");
            assertThat(text(call(bob, "list_tasks", Map.of()))).doesNotContain("Alice's secret task");
        }
    }

    @Test
    void wrongApiKeyIsRejected() {
        assertThatThrownBy(() -> {
            try (McpSyncClient client = connect("not-a-real-key")) {
                // connect() already initializes, so we never get here
            }
        }).hasStackTraceContaining("401");
    }

    // ---- helpers ----

    private McpSyncClient connect(String apiKey) {
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

    private static CallToolResult call(McpSyncClient client, String tool, Map<String, Object> arguments) {
        return client.callTool(new CallToolRequest(tool, arguments));
    }

    private static String text(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }

    private static String hash(String key) {
        return HexFormat.of().formatHex(ApiKeyAuthenticationFilter.sha256(key));
    }
}
