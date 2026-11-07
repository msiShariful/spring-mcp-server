# Spring MCP Server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server built with **Spring Boot** and **Spring AI**.
It exposes tools to MCP clients (Claude, IDEs, agents) over the **Streamable HTTP** transport.

[![CI](https://github.com/msiShariful/spring-mcp-server/actions/workflows/ci.yml/badge.svg)](https://github.com/msiShariful/spring-mcp-server/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen)
![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.1-blue)
![MCP](https://img.shields.io/badge/MCP-Streamable%20HTTP-purple)

---

## Table of contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Prerequisites](#prerequisites)
- [How to run](#how-to-run)
- [Configuration](#configuration)
- [Testing with MCP Inspector](#testing-with-mcp-inspector)
  - [Option A: Inspector in a Docker container](#option-a-inspector-in-a-docker-container)
  - [Option B: Inspector with npx](#option-b-inspector-with-npx)
  - [Option C: Inspector CLI (no UI)](#option-c-inspector-cli-no-ui)
- [Authentication](#authentication)
- [Connecting from Claude Code](#connecting-from-claude-code)
- [Adding a new tool](#adding-a-new-tool)
- [Troubleshooting](#troubleshooting)

---

## Features

| Capability | Name            | Description                         |
|------------|-----------------|-------------------------------------|
| Tool       | `get_current_weather(city)` | Live weather for any city: temperature, feels-like, humidity, wind, conditions. |
| Tool       | `get_forecast(city, days?)` | Daily forecast (1–16 days, default 3): min/max temperature, conditions, chance of rain. |
| Tool 🔒    | `create_task(title, description?, priority?, dueDate?)` | Add a task to the user's to-do list. |
| Tool 🔒    | `list_tasks(status?, priority?, overdueOnly?)` | List tasks sorted by due date, with optional filters. |
| Tool 🔒    | `get_task(taskId)` | Full details of one task. |
| Tool 🔒    | `update_task(taskId, title?, description?, status?, priority?, dueDate?)` | Change only the given fields. |
| Tool 🔒    | `complete_task(taskId)` | Mark a task as done. |
| Tool 🔒    | `delete_task(taskId)` | Permanently delete a task (marked as destructive). |
| Tool 🔒    | `task_summary()` | Totals by status, overdue and due today. |
| Resources  | _planned_       |                                     |
| Prompts    | _planned_       |                                     |

Weather data comes from [Open-Meteo](https://open-meteo.com), which is free and needs no API key.

🔒 = needs an API key. Each key belongs to one user, and users only see their own tasks (see [Authentication](#authentication)). Weather tools are public.
Tasks are stored in a local H2 database file (`./data/tasks.mv.db`), so they survive restarts.

Every tool declares MCP safety hints (`readOnlyHint`, `destructiveHint`, `openWorldHint`) so AI clients know which tools only read data, which can delete data, and which call outside services.

## Tech stack

- **Java 25**
- **Spring Boot 4.1.2**
- **Spring AI 2.0.1**, using `spring-ai-starter-mcp-server-webmvc`
- **Maven** (wrapper included, so you don't need to install Maven)

## Project structure

```
spring-mcp/
├── pom.xml
├── mvnw / mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/islamshariful/spring_mcp/
    │   │   ├── SpringMcpApplication.java   # Spring Boot entry point
    │   │   ├── security/
    │   │   │   ├── SecurityConfig.java     # Spring Security setup
    │   │   │   ├── ApiKeyAuthenticationFilter.java # Checks the API key on each request
    │   │   │   └── ApiKeyProperties.java   # Reads app.security.api-keys
    │   │   ├── user/
    │   │   │   ├── CurrentUserProvider.java # "Who is calling?"
    │   │   │   └── SecurityUserProvider.java # Returns the logged-in user, refuses anonymous callers
    │   │   ├── tasks/
    │   │   │   ├── TaskTools.java          # @McpTool definitions (task tools)
    │   │   │   ├── TaskService.java        # Business logic, always scoped to the current user
    │   │   │   ├── TaskRepository.java     # Spring Data JPA queries
    │   │   │   └── Task.java               # JPA entity
    │   │   └── weather/
    │   │       ├── WeatherTools.java       # @McpTool definitions (weather tools)
    │   │       ├── OpenMeteoClient.java    # Calls the Open-Meteo API
    │   │       └── WeatherCodes.java       # Weather code -> text (e.g. 61 -> "Light rain")
    │   └── resources/
    │       └── application.yaml            # MCP server configuration
    └── test/
        └── java/com/islamshariful/spring_mcp/
            ├── SpringMcpApplicationTests.java
            ├── tasks/TaskServiceTest.java  # Includes a test that users can't see each other's tasks
            └── security/McpAuthenticationTest.java # End-to-end auth test using the MCP Java client
```

## Prerequisites

| Tool              | Version | Needed for                          |
|-------------------|---------|-------------------------------------|
| JDK               | 25+     | Building and running the server     |
| Docker            | any     | Inspector Option A                  |
| Node.js (npx)     | 22+     | Inspector Options B and C           |

Check your Java version:

```bash
java -version
```

## How to run

### 1. Clone

```bash
git clone git@github.com:msiShariful/spring-mcp-server.git
cd spring-mcp-server
```

### 2. Start the server

**Development mode:**

```bash
./mvnw spring-boot:run
```

**Or build a jar and run it:**

```bash
./mvnw clean package
java -jar target/spring-mcp-0.0.1-SNAPSHOT.jar
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

### 3. Confirm it's up

Look for this line in the log:

```
Tomcat started on port 8080
```

The MCP endpoint is now available at:

```
http://localhost:8080/mcp
```

> Opening this URL in a browser won't show a page. It's an MCP endpoint, so use an MCP client (see below).

### 4. Run the unit tests

```bash
./mvnw test
```

## Configuration

`src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: spring-mcp
  ai:
    mcp:
      server:
        protocol: streamable   # Streamable HTTP transport
        stdio: false           # HTTP only, no stdio
        type: sync             # synchronous tool handlers
        name: mcp-server
        version: 1.0.0
```

| Property                                               | Default | Purpose                      |
|--------------------------------------------------------|---------|------------------------------|
| `server.port`                                          | `8080`  | HTTP port                    |
| `spring.ai.mcp.server.streamable-http.mcp-endpoint`   | `/mcp`  | Path of the MCP endpoint     |

To use a different port:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

---

## Testing with MCP Inspector

[MCP Inspector](https://github.com/modelcontextprotocol/inspector) is the official tool for testing MCP servers interactively.
**Start the Spring server first** (see [How to run](#how-to-run)).

### Option A: Inspector in a Docker container

```bash
docker run --rm -p 127.0.0.1:6274:6274 ghcr.io/modelcontextprotocol/inspector
```

The container logs print a URL containing an `MCP_INSPECTOR_API_TOKEN`. Open that URL in your browser.

> **Important:** inside the container, `localhost` means the container itself, not your machine.
> Use **`host.docker.internal`** to reach the Spring server running on your host.

In the Inspector:

1. Click **Add Servers** and add a server manually.
2. Fill in:

   | Field          | Value                                     |
   |----------------|-------------------------------------------|
   | Transport type | **Streamable HTTP**                       |
   | URL            | `http://host.docker.internal:8080/mcp`    |

3. Save, then turn on the server's toggle. The status should change to **Connected**.
4. Open the **Tools** tab, click **List Tools**, select `get_current_weather`, enter a city (e.g. `Dhaka`), and click **Run Tool**.

Expected result:

```json
{
  "content": [{
    "type": "text",
    "text": "{\"location\":\"Dhaka, Bangladesh\",\"temperatureC\":29.3,\"feelsLikeC\":33.1,\"humidityPercent\":58,\"windSpeedKmh\":6.8,\"conditions\":\"Clear sky\", ...}"
  }],
  "isError": false
}
```

**Useful extras:**

```bash
# Keep your saved servers between runs
docker run --rm -p 127.0.0.1:6274:6274 \
  -v mcp-inspector-data:/home/node/.mcp-inspector \
  ghcr.io/modelcontextprotocol/inspector

# Use a fixed token instead of a random one
docker run --rm -p 127.0.0.1:6274:6274 \
  -e MCP_INSPECTOR_API_TOKEN=my-local-token \
  ghcr.io/modelcontextprotocol/inspector
```

> On **Linux**, `host.docker.internal` isn't defined by default. Add
> `--add-host=host.docker.internal:host-gateway` to the `docker run` command.

### Option B: Inspector with npx

```bash
npx @modelcontextprotocol/inspector
```

It opens in your browser automatically. Follow the same steps as Option A, but use this URL:

```
http://localhost:8080/mcp
```

### Option C: Inspector CLI (no UI)

This option works well for quick checks and CI scripts.

```bash
# List all tools
npx @modelcontextprotocol/inspector --cli http://localhost:8080/mcp --method tools/list

# Current weather in Dhaka
npx @modelcontextprotocol/inspector --cli http://localhost:8080/mcp \
  --method tools/call --tool-name get_current_weather --tool-arg city=Dhaka

# 5-day forecast for London
npx @modelcontextprotocol/inspector --cli http://localhost:8080/mcp \
  --method tools/call --tool-name get_forecast --tool-arg city=London --tool-arg days=5
```

The same commands work through Docker:

```bash
docker run --rm ghcr.io/modelcontextprotocol/inspector \
  --cli http://host.docker.internal:8080/mcp --method tools/list
```

---

## Authentication

Task tools need an **API key**. Weather tools stay public.

| Request | Result |
|---|---|
| No key | Weather tools work. Task tools return *"Authentication required"*. |
| Valid key | Logged in as the key's user. Task tools only see that user's tasks. |
| Wrong key | Rejected with HTTP **401** `{"error":"Invalid API key"}` |

Send the key in either header:

```
Authorization: Bearer <your-api-key>
X-API-Key: <your-api-key>
```

### Managing keys

Keys are listed in `application.yaml`. Only the **SHA-256 hash** of each key is stored, so the file is safe to commit:

```yaml
app:
  security:
    api-keys:
      - user: sharif
        sha256: 2bbe1ef0...
```

To add a user, create a key and its hash, then add a new entry:

```bash
KEY="mcp_$(openssl rand -hex 24)"; echo "Key: $KEY"
printf %s "$KEY" | shasum -a 256
```

Give the key to the user and keep it secret. To revoke a key, delete its entry and restart.

### Using a key

**MCP Inspector:** in the server's connection settings, add a header named `Authorization` with the value `Bearer <your-api-key>`, then reconnect.

**Inspector CLI:**

```bash
npx @modelcontextprotocol/inspector --cli http://localhost:8080/mcp \
  --method tools/call --tool-name list_tasks \
  --header "Authorization: Bearer $MCP_API_KEY"
```

### How it works

1. `ApiKeyAuthenticationFilter` hashes the key from the request and looks it up (constant-time compare).
2. A valid key logs the request in as that user. No sessions or cookies are used.
3. Task tools ask `CurrentUserProvider` who is calling. `SecurityUserProvider` returns the logged-in user or refuses anonymous calls.
4. Every task query filters by that user, so one user can never read, change, or delete another user's tasks.

`McpAuthenticationTest` checks all of this end to end with the official MCP Java client.

To move to OAuth2/JWT later, replace the API key filter with `spring-boot-starter-oauth2-resource-server`. The tools and `SecurityUserProvider` stay the same.

---

## Connecting from Claude Code

```bash
claude mcp add --transport http spring-mcp http://localhost:8080/mcp \
  --header "Authorization: Bearer $MCP_API_KEY"
```

Leave out `--header` to use only the public weather tools.
Then run `/mcp` inside Claude Code to check that the server is connected.

---

## Adding a new tool

Add a method annotated with `@McpTool` to a Spring `@Component`. Spring AI registers it automatically.
Use `@McpToolParam` to describe each input. This example is from `WeatherTools.java`:

```java
@Component
public class WeatherTools {

    @McpTool(name = "get_current_weather",
            description = "Get the current weather (temperature, feels-like, humidity, wind, conditions) for a city.")
    public CurrentWeather getCurrentWeather(
            @McpToolParam(description = "City name, e.g. 'Dhaka' or 'London'") String city) {
        // call the API and return a record; Spring AI sends it to the AI as JSON
    }
}
```

Restart the server, then click **List Tools** in the Inspector again to see the new tool.

> Write clear `description` text. The AI model reads it to decide when to call the tool.

---

## Troubleshooting

| Problem                                         | Fix                                                                                         |
|-------------------------------------------------|---------------------------------------------------------------------------------------------|
| `Port 8080 was already in use`                  | Another instance is already running. Stop it (`lsof -i :8080`) or change `server.port`.     |
| Inspector: **Connection refused**               | The Spring server isn't running, or (in Docker) you used `localhost` instead of `host.docker.internal`. |
| Inspector: **404 Not Found**                    | Wrong path. The endpoint is `/mcp` unless you changed `mcp-endpoint`.                      |
| Inspector connects but the call fails           | The transport type must be **Streamable HTTP**, not SSE or STDIO.                          |
| Task tool: **Authentication required**          | Add the `Authorization: Bearer <key>` header and reconnect.                                 |
| HTTP **401 Invalid API key**                    | The key is wrong or was removed from `app.security.api-keys`. Check for typos or extra spaces. |
| Build fails with `invalid target release: 25`   | Install JDK 25 and point `JAVA_HOME` to it.                                                 |

---

## Contributing

Issues and pull requests are welcome. For bigger changes, please open an issue first to discuss what you'd like to change.

1. Fork the repo and create a branch: `git checkout -b feature/my-tool`
2. Make your change and run `./mvnw verify`
3. Open a pull request against `master`

## License

Released under the [MIT License](LICENSE).
