# MyBlog AI Cloud Remaining Milestones Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成智能博客微服务平台的可演示 MVP：用户鉴权、文章 CRUD、Redis 缓存、Elasticsearch 搜索、RocketMQ AI 摘要任务、FastAPI SSE 对话。

**Architecture:** Blog Service 负责业务数据、缓存、搜索索引、AI 任务状态；Gateway 保持统一入口；AI Agent Service 提供摘要、任务消费和 SSE 流。第一轮采用 JDBC/MySQL 明确数据流，AI 结果先用可替换的本地摘要器保证无外部密钥也能演示。

**Tech Stack:** Spring Boot Web/JDBC/Redis/Validation、Spring Cloud Gateway/Nacos/LoadBalancer、MySQL、Redis、Elasticsearch Java Client、RocketMQ Spring、FastAPI、httpx、sse-starlette。

---

## Task 1: Blog Core

**Files:** modify `services/blog-service/pom.xml`; create `db/schema.sql`; create Java packages `common`, `auth`, `article`, `cache`.

- [ ] Add tests for auth registration/login and article create/read/update/delete using service-level fake repositories.
- [ ] Implement DTOs, controllers, services, repositories, schema, and simple HMAC token utility.
- [ ] Verify with `mvn -q -pl services/blog-service test` and `mvn -q -DskipTests package`.
- [ ] Commit as `feat: add blog auth and article core`.

## Task 2: Redis Article Cache

**Files:** modify article service; create cache tests.

- [ ] Add tests proving article detail reads cache after first load and update invalidates cache.
- [ ] Implement Redis-backed `ArticleCacheService` with JSON serialization failure isolation.
- [ ] Verify with Blog Service tests and package.
- [ ] Commit as `feat: add article redis cache`.

## Task 3: Elasticsearch Search

**Files:** create `search` package; update article publish/update/delete flow.

- [ ] Add tests for search document mapping and degraded search failure response.
- [ ] Implement index sync service and `/api/blog/search`.
- [ ] Verify with unit tests and package; document index name `blog_article`.
- [ ] Commit as `feat: add article search integration`.

## Task 4: AI Task Flow

**Files:** create `ai` and `mq` packages in Blog Service; create `mq` and `clients` packages in AI Agent Service.

- [ ] Add tests for AI task status transitions: `PENDING -> PROCESSING -> SUCCESS` and failure path.
- [ ] Implement task table, submit API, result callback API, RocketMQ producer abstraction, and Python consumer entrypoint.
- [ ] Keep local fallback mode so API works when RocketMQ is not running.
- [ ] Verify Java tests, Python compile, and package.
- [ ] Commit as `feat: add async ai summary flow`.

## Task 5: SSE AI Chat

**Files:** modify AI Agent Service routes; update Gateway route docs.

- [ ] Add FastAPI tests for `/health`, `/api/ai/chat/stream`, and summary endpoint.
- [ ] Implement SSE stream endpoint on `/api/ai/chat/stream`.
- [ ] Verify with `python -m compileall app` and a curl smoke test when service is running.
- [ ] Commit as `feat: add ai streaming endpoint`.

## Task 6: End-to-End Verification

**Files:** update `README.md`.

- [ ] Start Docker Compose, Blog, Gateway, and AI services.
- [ ] Verify auth, article CRUD, Gateway routing, search degraded behavior, AI summary fallback, and SSE response.
- [ ] Stop all runtime processes and containers.
- [ ] Commit README demo commands as `docs: add full mvp verification`.

## Acceptance Checks

- `docker compose -f deploy/docker-compose.yml config` exits `0`.
- `mvn -q test` exits `0`.
- `mvn -q -DskipTests package` exits `0`.
- `python -m compileall app` exits `0`.
- Gateway health route returns Blog Service `UP`.
- AI health route returns AI Agent Service `UP`.
- Article CRUD and AI task APIs return JSON responses suitable for frontend integration.
