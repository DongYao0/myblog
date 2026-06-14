# MyBlog AI Cloud

基于 Spring Cloud Alibaba 与 FastAPI AI Agent 的智能博客微服务平台。

## 第一阶段目标

- 使用 Docker Compose 启动 Nacos、MySQL、Redis、RocketMQ、Elasticsearch。
- 启动 Gateway Service、Blog Service、AI Agent Service。
- 通过 Gateway 访问 Blog Service 健康检查。
- 直接访问 AI Agent Service 健康检查。

## 目录结构

```text
services/
  gateway-service/
  blog-service/
  ai-agent-service/
web/
  admin-console/
deploy/
  docker-compose.yml
  mysql/init/
docs/
  superpowers/
```

## 启动基础设施

```powershell
docker compose -f deploy/docker-compose.yml up -d
docker compose -f deploy/docker-compose.yml ps
```

## 本地端口

- Admin Console：`5173`
- Gateway Service：`18080`
- Blog Service：`18081`
- AI Agent Service：`18000`
- Nacos：`8848`
- MySQL：`13306`
- Redis：`16379`
- RocketMQ NameServer：`9876`
- RocketMQ Broker：`10911`、`10909`
- Elasticsearch：`9200`

## 验证接口

```powershell
curl.exe -m 10 http://localhost:18080/api/blog/health
curl.exe -m 10 http://localhost:18000/health
```

## RocketMQ AI 任务链路

- Blog Service 提交摘要任务后向 `ai-task-topic` 发送 `AI_SUMMARY_REQUESTED` 消息。
- Blog Service 内置消费者消费该消息，并把任务状态推进为 `PROCESSING -> SUCCESS`。
- 如果 RocketMQ 不可用，调度器会自动回退到本地执行，保证演示链路不中断。

## 第一阶段验收

- `docker compose -f deploy/docker-compose.yml config` 可以通过。
- `mvn -q -DskipTests package` 可以通过。
- `python -m compileall app` 可以通过。
- Blog Service 健康检查：`GET http://localhost:18080/api/blog/health`。
- AI Agent Service 健康检查：`GET http://localhost:18000/health`。

## MVP 演示命令

```powershell
curl.exe -m 10 -X POST http://localhost:18080/api/blog/auth/register -H "Content-Type: application/json" -d "{\"username\":\"commander\",\"password\":\"secret123\"}"
$login = Invoke-RestMethod -TimeoutSec 10 -Method Post -Uri "http://localhost:18080/api/blog/auth/login" -ContentType "application/json" -Body '{"username":"commander","password":"secret123"}'
$token = $login.token
Invoke-RestMethod -TimeoutSec 10 -Method Post -Uri "http://localhost:18080/api/blog/articles" -Headers @{Authorization="Bearer $token"} -ContentType "application/json" -Body '{"title":"Spring Cloud AI Blog","content":"Gateway Nacos Redis RocketMQ Elasticsearch FastAPI","authorId":1}'
Invoke-RestMethod -TimeoutSec 10 -Method Get -Uri "http://localhost:18080/api/blog/articles/1" -Headers @{Authorization="Bearer $token"}
Invoke-RestMethod -TimeoutSec 10 -Method Get -Uri "http://localhost:18080/api/blog/search?keyword=Spring" -Headers @{Authorization="Bearer $token"}
Invoke-RestMethod -TimeoutSec 10 -Method Post -Uri "http://localhost:18080/api/blog/ai/tasks/summary" -Headers @{Authorization="Bearer $token"} -ContentType "application/json" -Body '{"articleId":1}'
Invoke-RestMethod -TimeoutSec 10 -Method Post -Uri "http://localhost:18080/api/ai/agent/chat" -Headers @{Authorization="Bearer $token"} -ContentType "application/json" -Body '{"message":"请计算 6 * 7"}'
curl.exe -m 10 -X POST http://localhost:18000/api/ai/summary -H "Content-Type: application/json" -d "{\"content\":\"one two three four five six\"}"
curl.exe -m 10 "http://localhost:18000/api/ai/chat/stream?message=hello%20ai"
```

## 前端管理台

```powershell
cd web/admin-console
npm install
npm run dev
```

浏览器访问 `http://127.0.0.1:5173`，登录后可以发布文章、全文搜索、提交 RocketMQ 摘要任务、调用 Agent 工具和查看流式输出。如本机 `5173` 已被占用，可执行 `npm run dev -- --port 15173`。
