# MyBlog AI Cloud Design

Date: 2026-06-14

## Goal

Build an interview-ready intelligent blog microservice platform that combines Spring Cloud Alibaba with a Python AI Agent. The first deliverable must be runnable by one developer and demonstrate a complete engineering loop: gateway access, service discovery, blog CRUD, Redis cache, RocketMQ async AI tasks, Elasticsearch search, FastAPI AI service, and Docker Compose deployment.

## Scope

In scope for the first version:

- Spring Cloud Gateway as the unified HTTP entry.
- Nacos for service discovery and centralized configuration.
- Blog Service for users, articles, comments, tags, search, and AI task state.
- FastAPI AI Agent Service for article summary, article Q&A, and streaming chat.
- Redis for hot article cache, login/token auxiliary data, idempotency keys, and distributed locks.
- RocketMQ for long-running AI summary tasks.
- Elasticsearch for article full-text search.
- Docker Compose for local one-command infrastructure startup.
- Minimal frontend-ready APIs, even if the first pass is backend-first.

Out of scope for the first version:

- Kubernetes.
- Seata distributed transactions.
- Redis Cluster.
- MySQL primary-replica deployment.
- SkyWalking, Prometheus, and Grafana.
- Complex recommendation systems or content moderation.

## Architecture

```text
Frontend or API Client
        |
        v
Spring Cloud Gateway
  - JWT auth filter
  - CORS
  - route forwarding
  - basic rate limiting
        |
        +----------------------+
        |                      |
        v                      v
Blog Service              AI Agent Service
Spring Boot               FastAPI
MyBatis-Plus              LangChain or direct LLM adapter
Redis client              SSE streaming endpoints
RocketMQ producer         RocketMQ consumer
Elasticsearch client      Article tool APIs
        |
        v
MySQL / Redis / RocketMQ / Elasticsearch / Nacos
```

## Services

### Gateway Service

Responsibilities:

- Route `/api/blog/**` to Blog Service.
- Route `/api/ai/**` to AI Agent Service.
- Validate JWT for protected routes.
- Allow public access for login, register, article list, article detail, and search.
- Add request metadata such as user id and trace id headers when available.
- Apply simple route-level rate limits, especially for AI endpoints.

Dependencies:

- Nacos discovery.
- Redis for optional gateway rate-limit storage.

### Blog Service

Responsibilities:

- User registration and login.
- Article CRUD.
- Article detail cache with Redis.
- Comment and tag management.
- Article search through Elasticsearch.
- AI task creation and state query.
- RocketMQ message production for asynchronous AI summary tasks.
- Persist AI generated summary result after callback or pull update.

Core modules:

- `auth`: user account, password hash, JWT issue.
- `article`: article CRUD, publish state, tags.
- `search`: Elasticsearch index sync and query.
- `cache`: Redis cache read/write and invalidation.
- `ai-task`: task submit, status transition, result storage.
- `mq`: RocketMQ producer and message model.

### AI Agent Service

Responsibilities:

- Provide synchronous article summary API for small content.
- Provide asynchronous RocketMQ consumer for long summary tasks.
- Provide SSE streaming chat endpoint.
- Provide article Q&A endpoint.
- Call Blog Service tool APIs when it needs article context.
- Isolate LLM failures and return stable degraded responses.

Core modules:

- `api`: FastAPI routers.
- `agent`: Agent orchestration and tool calling.
- `llm`: LLM provider adapter.
- `mq`: RocketMQ consumer.
- `clients`: Blog Service HTTP client.
- `schemas`: request and response models.

## Data Flow

### Article Detail Cache

1. Client requests `GET /api/blog/articles/{id}` through Gateway.
2. Blog Service checks Redis key `article:detail:{id}`.
3. Cache hit returns cached article data.
4. Cache miss reads MySQL, writes Redis with TTL, then returns data.
5. Article update or delete invalidates related Redis keys and updates Elasticsearch.

### Article Search

1. Client requests `GET /api/blog/search?keyword=...`.
2. Blog Service queries Elasticsearch article index.
3. Search result returns article id, title, highlight snippet, tags, and publish time.
4. If Elasticsearch fails, Blog Service returns a clear degraded error instead of silently falling back to slow broad MySQL LIKE.

### Async AI Summary

1. Client requests `POST /api/blog/ai/tasks/summary` with article id.
2. Blog Service validates article ownership or visibility.
3. Blog Service uses Redis lock `lock:ai-summary:{articleId}` to prevent duplicate submissions.
4. Blog Service creates an AI task in MySQL with status `PENDING`.
5. Blog Service sends RocketMQ message `AI_SUMMARY_REQUESTED`.
6. AI Agent Service consumes the message and loads article content through Blog Service API.
7. AI Agent Service calls the LLM and produces a summary.
8. AI Agent Service updates Blog Service task result through an internal API.
9. Client polls `GET /api/blog/ai/tasks/{taskId}` until status is `SUCCESS` or `FAILED`.

Status model:

- `PENDING`: task has been created.
- `PROCESSING`: AI service has started work.
- `SUCCESS`: result is available.
- `FAILED`: task failed with an error message.

### SSE AI Chat

1. Client connects to `GET /api/ai/chat/stream`.
2. Gateway routes the request to AI Agent Service and keeps the streaming response open.
3. AI Agent Service streams tokens as SSE events.
4. If LLM call fails, AI Agent Service emits an error event and closes the stream.

## Storage Design

Minimum MySQL tables:

- `user`: account, password hash, role, created time.
- `article`: title, content, summary, author id, status, counters, timestamps.
- `tag`: tag name.
- `article_tag`: article-tag relation.
- `comment`: article comments.
- `ai_task`: task type, article id, status, result, error message, timestamps.

Redis key conventions:

- `article:detail:{articleId}`.
- `article:hot:list`.
- `lock:ai-summary:{articleId}`.
- `auth:refresh:{userId}` if refresh tokens are implemented.

Elasticsearch index:

- `blog_article`.
- Fields: `id`, `title`, `content`, `summary`, `tags`, `authorId`, `status`, `createdAt`, `updatedAt`.

RocketMQ topics:

- `ai-task-topic`.
- Tags: `AI_SUMMARY_REQUESTED`.

## Error Handling

- Gateway rejects invalid JWT with `401`.
- Gateway rejects excessive AI calls with `429`.
- Blog Service returns `404` for missing articles and `409` for duplicate AI task submission.
- Redis failure must not break article reads; Blog Service can read MySQL directly and skip cache write.
- Elasticsearch failure returns a search-specific degraded error.
- RocketMQ send failure marks AI task as `FAILED_TO_QUEUE` or returns submission failure before task is accepted.
- AI Agent Service catches LLM timeout and provider errors, marks the task as `FAILED`, and stores a user-readable error message.
- SSE endpoints must emit an error event before closing when streaming fails after the response starts.

## Testing Strategy

- Unit tests for article cache logic, AI task status transitions, and JWT utility.
- Integration tests for Blog Service with MySQL, Redis, RocketMQ, and Elasticsearch containers where practical.
- FastAPI tests for summary, chat stream, and callback failure paths.
- Gateway route tests for public and protected endpoints.
- Docker Compose smoke test: infrastructure starts, services register to Nacos, and basic article and AI task flows work.

## Milestones

### Milestone 1: Infrastructure and Skeleton

- Repository structure.
- Docker Compose for Nacos, MySQL, Redis, RocketMQ, Elasticsearch.
- Gateway Service skeleton.
- Blog Service skeleton.
- AI Agent Service skeleton.

### Milestone 2: Blog Core

- User auth.
- Article CRUD.
- Redis article cache.
- Basic Gateway routes and JWT validation.

### Milestone 3: Search

- Elasticsearch article index.
- Article publish/update/delete index sync.
- Search API.

### Milestone 4: AI Async Flow

- AI task table.
- RocketMQ producer in Blog Service.
- RocketMQ consumer in AI Agent Service.
- AI result update API.
- Task status query API.

### Milestone 5: AI Streaming

- SSE chat endpoint.
- Gateway route support for streaming.
- Basic Agent tool for article lookup.

## Resume Positioning

Project title:

`Intelligent Blog Microservice Platform Based on Spring Cloud Alibaba and FastAPI AI Agent`

Resume emphasis:

- Built a microservice blog platform with Spring Cloud Gateway, Nacos, Redis, RocketMQ, Elasticsearch, and Docker Compose.
- Designed an asynchronous AI summary workflow using RocketMQ to decouple long-running LLM calls from user requests.
- Implemented Redis hot article caching and distributed duplicate-submit protection for AI tasks.
- Integrated Elasticsearch full-text search for article title, content, tag, and summary queries.
- Built a FastAPI AI Agent service with streaming output and internal tool calls for article context retrieval.

## Acceptance Criteria

- `docker compose up` starts all infrastructure dependencies.
- Gateway can route requests to Blog Service and AI Agent Service.
- Blog Service registers in Nacos.
- AI Agent Service is discoverable or has a documented Nacos registration strategy.
- Article CRUD works through Gateway.
- Article detail cache can be verified through Redis keys.
- Search returns Elasticsearch results.
- AI summary task can be submitted, consumed, processed, and queried.
- SSE chat endpoint streams events.
- README documents startup steps and demo API calls.
