package com.islamshariful.spring_mcp.security;

import com.islamshariful.spring_mcp.McpTestSupport;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static com.islamshariful.spring_mcp.McpTestSupport.ALICE_KEY;
import static com.islamshariful.spring_mcp.McpTestSupport.BOB_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end test: starts the real server and talks to it with the official MCP Java client,
 * exactly like Claude or the MCP Inspector would.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:auth-test")
class McpAuthenticationTest {

    @DynamicPropertySource
    static void apiKeys(DynamicPropertyRegistry registry) {
        McpTestSupport.registerTestApiKeys(registry);
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
        return McpTestSupport.connect(port, apiKey);
    }

    private static CallToolResult call(McpSyncClient client, String tool, Map<String, Object> arguments) {
        return client.callTool(new CallToolRequest(tool, arguments));
    }

    private static String text(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }
}
