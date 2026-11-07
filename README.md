# Spring MCP Server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server built with **Spring Boot** and **Spring AI**.
It exposes tools to MCP clients (Claude, IDEs, agents) over the **Streamable HTTP** transport.

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
- [Connecting from Claude Code](#connecting-from-claude-code)
- [Adding a new tool](#adding-a-new-tool)
- [Troubleshooting](#troubleshooting)

---

## Features

| Capability | Name            | Description                         |
|------------|-----------------|-------------------------------------|
| Tool       | `latest_videos` | Returns the list of latest videos.  |
| Tool       | `get_current_weather(city)` | Live weather for any city: temperature, feels-like, humidity, wind, conditions. |
| Tool       | `get_forecast(city, days?)` | Daily forecast (1–16 days, default 3): min/max temperature, conditions, chance of rain. |
| Resources  | _planned_       |                                     |
| Prompts    | _planned_       |                                     |

Weather data comes from [Open-Meteo](https://open-meteo.com), which is free and needs no API key.

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
    │   │   └── McpTools.java               # @McpTool definitions
    │   └── resources/
    │       └── application.yaml            # MCP server configuration
    └── test/
        └── java/com/islamshariful/spring_mcp/
            └── SpringMcpApplicationTests.java
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
4. Open the **Tools** tab, click **List Tools**, select `latest_videos`, and click **Run Tool**.

Expected result:

```json
{
  "content": [{ "type": "text", "text": "This is a latest videos list" }],
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

# Call the latest_videos tool
npx @modelcontextprotocol/inspector --cli http://localhost:8080/mcp \
  --method tools/call --tool-name latest_videos
```

The same commands work through Docker:

```bash
docker run --rm ghcr.io/modelcontextprotocol/inspector \
  --cli http://host.docker.internal:8080/mcp --method tools/list
```

---

## Connecting from Claude Code

```bash
claude mcp add --transport http spring-mcp http://localhost:8080/mcp
```

Then run `/mcp` inside Claude Code to check that the server is connected.

---

## Adding a new tool

Add a method annotated with `@McpTool` to a Spring `@Component`. Spring AI registers it automatically.

```java
@Component
public class McpTools {

    @McpTool(name = "latest_videos", description = "This is the tool with latest videos list")
    public String getLatestVideos() {
        return "This is a latest videos list";
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
| Build fails with `invalid target release: 25`   | Install JDK 25 and point `JAVA_HOME` to it.                                                 |

---

## License

This project is for learning and demonstration purposes.
