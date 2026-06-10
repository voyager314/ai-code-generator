# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**wise-code** is a Spring Boot application that generates front-end code (HTML, multi-file HTML/CSS/JS, or Vue 3 projects) from natural language prompts using an LLM-driven agentic workflow. It uses DeepSeek as the LLM provider via LangChain4j, and LangGraph4j for orchestrating multi-step code generation pipelines.

## Build & Run

```bash
# Build (skip tests — only a context-loads test exists)
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run

# Run tests
./mvnw test
```

- **Java 21** required
- Server starts on **port 8081**, context path `/api`
- Active profile defaults to `local` (see `application-local.yml`)
- Requires **MySQL** (localhost:3306/ai_code_generator) and **Redis** to be running

## Architecture

### AI Workflow (LangGraph4j)

The core pipeline is a LangGraph4j state machine defined in `ai/WorkFlow/WorkFlowApp.java`:

```
START → ImageCollect → PromptEnhance → Router → CodeGenerate → CodeQualityCheck
                                                                    ├→ FAIL (retry up to 5×) → CodeGenerate
                                                                    ├→ SKIP_BUILD (HTML/multi-file) → END
                                                                    └→ BUILD (Vue) → ProjectBuild → END
```

- **WorkFlowContext** (`ai/WorkFlow/state/`) — shared mutable state passed through the graph
- **Nodes** (`ai/WorkFlow/node/`) — each node reads/writes WorkFlowContext
- **Tools** (`ai/WorkFlow/tools/`) — ImageSearchTool (Pexels API), LogoGenerateTool (DashScope), MermaidTool (Selenium-based rendering)

### Code Generation Types

Routing is LLM-driven (`AiRoutingService`). Three generation paths:

| Type | Service | Parser | Saver | Output |
|------|---------|--------|-------|--------|
| `HTML` | Single-file HTML+CSS+JS | `HtmlCodeParser` | `HtmlCodeSaver` | One .html file |
| `MULTI_FILE` | Separate HTML/CSS/JS | `MultiFileCodeParser` | `MultiFileCodeSaver` | Multiple files |
| `VUE_PROJECT` | Full Vue 3 + Vite project | `MultiFileCodeParser` | `MultiFileCodeSaver` | Project directory with npm build |

### Factory Pattern for AI Services

Per-app AI service instances are cached in Caffeine caches (30min TTL) via factory classes:
- `AiCodeGenServiceFactory` — creates LangChain4j AiServices with different models/tools per CodeGenType
- `AiRoutingServiceFactory`, `CodeQualityCheckFactory`, `ImageCollectionServiceFactory`

Vue project generation uses the `reasoningStreamingChatModel` bean (supports tool calling); other types use the default `streamingChatModel`.

### API Layer

Four controllers under `controller/`:
- **AppController** — app CRUD + SSE streaming code generation (`GET /app/chat/gen/code`) + deploy + download
- **UserController** — auth (session-based via Spring Session + Redis), user management
- **ChatHistoryController** — conversation history
- **HealthController** — `GET /health/`

Auth is annotation-driven: `@Auth(mustRole = "admin")` checked by AOP in `aop/`.

### Data Layer

- **MyBatis Flex** (not JPA) — entities in `entity/`, mappers in `mapper/`, XML in `resources/mapper/`
- Three tables: `app`, `user`, `chat_history` — all use logical delete (`isDelete`)

### Prompt Templates

System prompts live in `src/main/resources/prompt/*.md` — loaded by AI services at runtime. Key files:
- `router-prompt.md` — classification logic for code gen type
- `vue-project-prompt.md` — most complex; defines Vue 3 project structure constraints
- `quality-check-prompt.md` — reflection agent criteria

### External Services

- **DeepSeek** — LLM provider (OpenAI-compatible API via LangChain4j)
- **Redis** — chat memory (LangChain4j store), session storage, distributed caching (Redisson)
- **Aliyun OSS** — file/image storage (`AliOSSManager`)
- **Aliyun DashScope** — AI image generation (logo tool)
- **Pexels** — stock image search
- **Selenium** — headless browser for Mermaid diagram rendering

## Key Conventions

- DTOs use `*Request` suffix (in `dto/`), response objects use `*VO` suffix (in `vo/`)
- Streaming responses use `Flux<ServerSentEvent<String>>` via LangChain4j Reactor
- Rate limiting via `@RateLimit` annotation + AOP
- Business errors use `BusinessException` with `ErrorCode` enum
- Constants (output dirs, deploy dirs) centralized in `common/AppConstant`
