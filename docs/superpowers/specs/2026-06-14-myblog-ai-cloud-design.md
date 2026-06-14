# MyBlog AI Cloud 架构设计

日期：2026-06-14

## 目标

构建一个适合应届生求职展示的智能博客微服务平台。项目以 Spring Cloud Alibaba 为 Java 微服务底座，以 Python FastAPI AI Agent 为智能能力模块，第一版必须能由个人完整落地，并形成可演示闭环：网关入口、服务发现、博客 CRUD、Redis 缓存、RocketMQ 异步 AI 任务、Elasticsearch 搜索、FastAPI AI 服务、Docker Compose 本地部署。

## 第一版范围

必须落地：

- Spring Cloud Gateway 作为统一 HTTP 入口。
- Nacos 作为服务注册发现与配置中心。
- Blog Service 负责用户、文章、评论、标签、搜索与 AI 任务状态。
- FastAPI AI Agent Service 负责文章摘要、文章问答与流式对话。
- Redis 负责热点文章缓存、登录辅助数据、幂等键与分布式锁。
- RocketMQ 负责长耗时 AI 摘要任务。
- Elasticsearch 负责博客全文搜索。
- Docker Compose 负责本地一键启动基础设施。
- 提供可被前端接入的 API，即使第一阶段以后端优先。

暂不纳入第一版：

- Kubernetes。
- Seata 分布式事务。
- Redis Cluster。
- MySQL 主从。
- SkyWalking、Prometheus、Grafana。
- 复杂推荐系统与内容审核系统。

## 总体架构

```text
前端或 API 客户端
        |
        v
Spring Cloud Gateway
  - JWT 鉴权过滤器
  - 跨域处理
  - 路由转发
  - 基础限流
        |
        +----------------------+
        |                      |
        v                      v
Blog Service              AI Agent Service
Spring Boot               FastAPI
MyBatis-Plus              LangChain 或直接 LLM 适配器
Redis Client              SSE 流式接口
RocketMQ Producer         RocketMQ Consumer
Elasticsearch Client      文章工具接口
        |
        v
MySQL / Redis / RocketMQ / Elasticsearch / Nacos
```

## 服务划分

### Gateway Service

职责：

- 将 `/api/blog/**` 路由到 Blog Service。
- 将 `/api/ai/**` 路由到 AI Agent Service。
- 对受保护接口校验 JWT。
- 放行登录、注册、文章列表、文章详情、搜索等公开接口。
- 在有登录态时透传用户 ID、Trace ID 等请求元数据。
- 对 AI 接口做基础限流，避免大模型调用被刷爆。

依赖：

- Nacos 服务发现。
- Redis，可用于网关限流存储。

### Blog Service

职责：

- 用户注册与登录。
- 文章 CRUD。
- 使用 Redis 缓存文章详情。
- 评论与标签管理。
- 通过 Elasticsearch 查询文章。
- 创建 AI 任务并查询任务状态。
- 向 RocketMQ 投递异步 AI 摘要任务。
- 持久化 AI 生成的摘要结果。

核心模块：

- `auth`：用户账号、密码哈希、JWT 签发。
- `article`：文章 CRUD、发布状态、标签。
- `search`：Elasticsearch 索引同步与查询。
- `cache`：Redis 缓存读写与失效。
- `ai-task`：任务提交、状态流转、结果存储。
- `mq`：RocketMQ 生产者与消息模型。

### AI Agent Service

职责：

- 为短文本提供同步文章摘要接口。
- 消费 RocketMQ 中的长耗时 AI 摘要任务。
- 提供 SSE 流式对话接口。
- 提供文章问答接口。
- 需要文章上下文时调用 Blog Service 内部工具 API。
- 隔离 LLM 超时与供应商异常，返回稳定降级结果。

核心模块：

- `api`：FastAPI 路由。
- `agent`：Agent 编排与工具调用。
- `llm`：大模型供应商适配器。
- `mq`：RocketMQ 消费者。
- `clients`：Blog Service HTTP 客户端。
- `schemas`：请求与响应模型。

## 核心流程

### 文章详情缓存

1. 客户端通过 Gateway 请求 `GET /api/blog/articles/{id}`。
2. Blog Service 查询 Redis Key `article:detail:{id}`。
3. 命中缓存时直接返回文章数据。
4. 未命中时读取 MySQL，写入 Redis 并设置 TTL，然后返回数据。
5. 文章更新或删除时失效相关 Redis Key，并同步更新 Elasticsearch。

### 文章搜索

1. 客户端请求 `GET /api/blog/search?keyword=...`。
2. Blog Service 查询 Elasticsearch 的文章索引。
3. 返回文章 ID、标题、高亮摘要、标签与发布时间。
4. Elasticsearch 异常时返回明确的搜索降级错误，不静默退化为大范围 MySQL LIKE。

### 异步 AI 摘要

1. 客户端请求 `POST /api/blog/ai/tasks/summary`，提交文章 ID。
2. Blog Service 校验文章权限或可见性。
3. Blog Service 使用 Redis 锁 `lock:ai-summary:{articleId}` 防止重复提交。
4. Blog Service 在 MySQL 创建状态为 `PENDING` 的 AI 任务。
5. Blog Service 发送 RocketMQ 消息 `AI_SUMMARY_REQUESTED`。
6. AI Agent Service 消费消息，并通过 Blog Service API 读取文章内容。
7. AI Agent Service 调用 LLM 生成摘要。
8. AI Agent Service 调用 Blog Service 内部接口回写任务结果。
9. 客户端轮询 `GET /api/blog/ai/tasks/{taskId}`，直到状态为 `SUCCESS` 或 `FAILED`。

任务状态：

- `PENDING`：任务已创建。
- `PROCESSING`：AI 服务已开始处理。
- `SUCCESS`：结果已生成。
- `FAILED`：任务失败，并保存错误信息。

### SSE AI 对话

1. 客户端连接 `GET /api/ai/chat/stream`。
2. Gateway 将请求转发到 AI Agent Service，并保持流式响应。
3. AI Agent Service 以 SSE 事件流输出 token。
4. LLM 调用失败时，AI Agent Service 发送错误事件并关闭流。

## 存储设计

MySQL 最小表集合：

- `user`：账号、密码哈希、角色、创建时间。
- `article`：标题、正文、摘要、作者 ID、状态、计数器、时间戳。
- `tag`：标签名称。
- `article_tag`：文章与标签关系。
- `comment`：文章评论。
- `ai_task`：任务类型、文章 ID、状态、结果、错误信息、时间戳。

Redis Key 约定：

- `article:detail:{articleId}`。
- `article:hot:list`。
- `lock:ai-summary:{articleId}`。
- `auth:refresh:{userId}`，仅在实现 Refresh Token 时使用。

Elasticsearch 索引：

- 索引名：`blog_article`。
- 字段：`id`、`title`、`content`、`summary`、`tags`、`authorId`、`status`、`createdAt`、`updatedAt`。

RocketMQ Topic：

- Topic：`ai-task-topic`。
- Tag：`AI_SUMMARY_REQUESTED`。

## 错误处理

- Gateway 对无效 JWT 返回 `401`。
- Gateway 对过量 AI 请求返回 `429`。
- Blog Service 对不存在文章返回 `404`。
- Blog Service 对重复 AI 任务提交返回 `409`。
- Redis 异常不能阻断文章读取；Blog Service 可直接读 MySQL，并跳过缓存写入。
- Elasticsearch 异常返回搜索专用降级错误。
- RocketMQ 发送失败时，任务不得假装已受理；要么返回提交失败，要么标记为 `FAILED_TO_QUEUE`。
- AI Agent Service 捕获 LLM 超时和供应商异常，标记任务为 `FAILED`，并保存用户可读错误信息。
- SSE 流开始后失败时，必须先发送错误事件再关闭连接。

## 测试策略

- 单元测试覆盖文章缓存逻辑、AI 任务状态流转、JWT 工具类。
- Blog Service 集成测试覆盖 MySQL、Redis、RocketMQ、Elasticsearch 的关键路径。
- FastAPI 测试覆盖摘要接口、流式对话、回写失败路径。
- Gateway 测试覆盖公开接口与受保护接口的路由和鉴权。
- Docker Compose 冒烟测试验证基础设施启动、服务注册、文章流程与 AI 任务流程。

## 里程碑

### 里程碑 1：基础设施与项目骨架

- 建立仓库目录结构。
- 编写 Nacos、MySQL、Redis、RocketMQ、Elasticsearch 的 Docker Compose。
- 初始化 Gateway Service。
- 初始化 Blog Service。
- 初始化 AI Agent Service。

### 里程碑 2：博客核心能力

- 用户鉴权。
- 文章 CRUD。
- Redis 文章详情缓存。
- Gateway 基础路由与 JWT 校验。

### 里程碑 3：全文搜索

- 建立 Elasticsearch 文章索引。
- 发布、更新、删除文章时同步索引。
- 实现搜索 API。

### 里程碑 4：AI 异步任务

- 建立 AI 任务表。
- Blog Service 接入 RocketMQ 生产者。
- AI Agent Service 接入 RocketMQ 消费者。
- 实现 AI 结果回写 API。
- 实现任务状态查询 API。

### 里程碑 5：AI 流式对话

- 实现 SSE 对话接口。
- 确认 Gateway 支持流式转发。
- 为 Agent 增加文章查询工具。

## 简历定位

项目名称：

`基于 Spring Cloud Alibaba 与 FastAPI AI Agent 的智能博客微服务平台`

简历表达重点：

- 基于 Spring Cloud Gateway、Nacos、Redis、RocketMQ、Elasticsearch、Docker Compose 构建智能博客微服务平台。
- 使用 RocketMQ 设计异步 AI 摘要流程，解耦长耗时 LLM 调用与用户请求。
- 使用 Redis 实现热点文章缓存，并基于分布式锁防止 AI 任务重复提交。
- 集成 Elasticsearch 实现文章标题、正文、标签、摘要的全文搜索。
- 使用 FastAPI 构建 AI Agent 服务，支持流式输出与文章上下文工具调用。

## 验收标准

- `docker compose up` 能启动全部基础设施依赖。
- Gateway 能路由到 Blog Service 与 AI Agent Service。
- Blog Service 能注册到 Nacos。
- AI Agent Service 可被发现，或具备明确的 Nacos 注册策略。
- 文章 CRUD 能通过 Gateway 访问。
- 文章详情缓存能通过 Redis Key 验证。
- 搜索接口返回 Elasticsearch 查询结果。
- AI 摘要任务可以提交、消费、处理、查询。
- SSE 对话接口能持续输出事件流。
- README 写明启动步骤与演示 API。
