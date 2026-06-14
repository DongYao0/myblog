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

- Gateway Service：`18080`
- Blog Service：`18081`
- AI Agent Service：`18000`
- Nacos：`8848`
- MySQL：`13306`
- Redis：`16379`
- RocketMQ NameServer：`9876`
- RocketMQ Broker：`11011`、`11009`
- Elasticsearch：`9200`

## 验证接口

```powershell
curl.exe -m 10 http://localhost:18080/api/blog/health
curl.exe -m 10 http://localhost:18000/health
```

## 第一阶段验收

- `docker compose -f deploy/docker-compose.yml config` 可以通过。
- `mvn -q -DskipTests package` 可以通过。
- `python -m compileall app` 可以通过。
- Blog Service 健康检查：`GET http://localhost:18080/api/blog/health`。
- AI Agent Service 健康检查：`GET http://localhost:18000/health`。

## MVP 演示命令

```powershell
curl.exe -m 10 -X POST http://localhost:18080/api/blog/auth/register -H "Content-Type: application/json" -d "{\"username\":\"commander\",\"password\":\"secret123\"}"
curl.exe -m 10 -X POST http://localhost:18080/api/blog/auth/login -H "Content-Type: application/json" -d "{\"username\":\"commander\",\"password\":\"secret123\"}"
curl.exe -m 10 -X POST http://localhost:18080/api/blog/articles -H "Content-Type: application/json" -d "{\"title\":\"Spring Cloud AI Blog\",\"content\":\"Gateway Nacos Redis RocketMQ Elasticsearch FastAPI\",\"authorId\":1}"
curl.exe -m 10 http://localhost:18080/api/blog/articles/1
curl.exe -m 10 "http://localhost:18080/api/blog/search?keyword=Spring"
curl.exe -m 10 -X POST http://localhost:18080/api/blog/ai/tasks/summary -H "Content-Type: application/json" -d "{\"articleId\":1}"
curl.exe -m 10 http://localhost:18080/api/blog/ai/tasks/1
curl.exe -m 10 -X POST http://localhost:18000/api/ai/summary -H "Content-Type: application/json" -d "{\"content\":\"one two three four five six\"}"
curl.exe -m 10 "http://localhost:18000/api/ai/chat/stream?message=hello%20ai"
```
