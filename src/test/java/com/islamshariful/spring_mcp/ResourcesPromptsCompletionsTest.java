package com.islamshariful.spring_mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CompleteReference;
import io.modelcontextprotocol.spec.McpSchema.CompleteRequest;
import io.modelcontextprotocol.spec.McpSchema.CompleteRequest.CompleteArgument;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptReference;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ResourceReference;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;

import static com.islamshariful.spring_mcp.McpTestSupport.ALICE_KEY;
import static com.islamshariful.spring_mcp.McpTestSupport.BOB_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end tests for resources, prompts and autocomplete.
 * Weather lookups need the internet, so these tests only use parts that work offline.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:resources-prompts-test")
class ResourcesPromptsCompletionsTest {

    @DynamicPropertySource
    static void apiKeys(DynamicPropertyRegistry registry) {
        McpTestSupport.registerTestApiKeys(registry);
    }

    @LocalServerPort
    private int port;

    // ---- Resources ----

    @Test
    void listsResourcesAndTemplates() {
        try (McpSyncClient client = McpTestSupport.connect(port, null)) {
            assertThat(client.listResources().resources()).extracting(Resource::uri).contains("tasks://open");
            assertThat(client.listResourceTemplates().resourceTemplates()).extracting(ResourceTemplate::uriTemplate)
                    .contains("tasks://{id}", "weather://{city}");
        }
    }

    @Test
    void taskResourcesShowOnlyTheUsersOwnTasks() {
        try (McpSyncClient alice = McpTestSupport.connect(port, ALICE_KEY);
             McpSyncClient bob = McpTestSupport.connect(port, BOB_KEY)) {

            alice.callTool(new CallToolRequest("create_task", Map.of("title", "Pack suitcase")));

            assertThat(read(alice, "tasks://open")).contains("Pack suitcase");
            assertThat(read(bob, "tasks://open")).doesNotContain("Pack suitcase");
        }
    }

    @Test
    void taskResourcesRequireAnApiKey() {
        try (McpSyncClient anonymous = McpTestSupport.connect(port, null)) {
            assertThatThrownBy(() -> read(anonymous, "tasks://open"))
                    .hasMessageContaining("Authentication required");
        }
    }

    // ---- Prompts ----

    @Test
    void listsPromptsWithArguments() {
        try (McpSyncClient client = McpTestSupport.connect(port, null)) {
            List<Prompt> prompts = client.listPrompts().prompts();

            assertThat(prompts).extracting(Prompt::name).contains("plan_my_day", "weekly_review", "trip_prep");
            Prompt tripPrep = prompts.stream().filter(p -> p.name().equals("trip_prep")).findFirst().orElseThrow();
            assertThat(tripPrep.arguments()).extracting(arg -> arg.name() + ":" + arg.required())
                    .containsExactlyInAnyOrder("city:true", "days:false");
        }
    }

    @Test
    void fillsInPromptArguments() {
        try (McpSyncClient client = McpTestSupport.connect(port, null)) {
            GetPromptResult result = client.getPrompt(
                    new GetPromptRequest("trip_prep", Map.of("city", "London", "days", "5")));

            String text = ((TextContent) result.messages().getFirst().content()).text();
            assertThat(text).contains("traveling to London for 5 days").contains("get_forecast");
        }
    }

    // ---- Autocomplete ----

    @Test
    void suggestsPopularCitiesForShortInput() {
        try (McpSyncClient client = McpTestSupport.connect(port, null)) {
            // One letter: no live search yet, so it uses the built-in popular cities list.
            assertThat(complete(client, new PromptReference("plan_my_day"), "city", "D")).containsExactly("Dhaka", "Dubai");
        }
    }

    @Test
    void suggestsTripDays() {
        try (McpSyncClient client = McpTestSupport.connect(port, null)) {
            assertThat(complete(client, new PromptReference("trip_prep"), "days", "1")).containsExactly("1", "10", "14", "16");
        }
    }

    @Test
    void suggestsOnlyTheUsersOwnTaskIds() {
        try (McpSyncClient alice = McpTestSupport.connect(port, ALICE_KEY);
             McpSyncClient bob = McpTestSupport.connect(port, BOB_KEY);
             McpSyncClient anonymous = McpTestSupport.connect(port, null)) {

            alice.callTool(new CallToolRequest("create_task", Map.of("title", "Alice task")));
            List<String> aliceIds = complete(alice, new ResourceReference("tasks://{id}"), "id", "");

            assertThat(aliceIds).isNotEmpty();
            assertThat(complete(bob, new ResourceReference("tasks://{id}"), "id", "")).doesNotContainAnyElementsOf(aliceIds);
            assertThat(complete(anonymous, new ResourceReference("tasks://{id}"), "id", "")).isEmpty();
        }
    }

    // ---- helpers ----

    private static String read(McpSyncClient client, String uri) {
        return ((TextResourceContents) client.readResource(new ReadResourceRequest(uri)).contents().getFirst()).text();
    }

    private static List<String> complete(McpSyncClient client, CompleteReference ref,
                                         String argument, String typed) {
        CompleteResult result = client.completeCompletion(new CompleteRequest(ref, new CompleteArgument(argument, typed)));
        return result.completion().values();
    }
}
