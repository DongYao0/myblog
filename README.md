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

## 验证接口

```powershell
curl.exe -m 10 http://localhost:8080/api/blog/health
curl.exe -m 10 http://localhost:8000/health
```
