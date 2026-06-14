# MyBlog AI Cloud 里程碑 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建 MyBlog AI Cloud 的第一阶段项目骨架，让基础设施、Gateway Service、Blog Service、AI Agent Service 都具备可启动、可健康检查、可继续扩展的最小闭环。

**Architecture:** 第一阶段采用单仓库多服务结构：Java 侧使用 Maven 多模块管理 `gateway-service` 与 `blog-service`，Python 侧独立维护 `ai-agent-service`。基础设施统一放在 `deploy/docker-compose.yml`，开发者先启动 Nacos、MySQL、Redis、RocketMQ、Elasticsearch，再启动三个业务服务。

**Tech Stack:** Spring Boot 3.3.x、Spring Cloud 2023.x、Spring Cloud Alibaba 2023.x、Java 17、FastAPI、Python 3.12、Docker Compose、Nacos、MySQL 8、Redis 7、RocketMQ 5、Elasticsearch 8。

---

## 范围检查

完整设计包含博客核心、搜索、异步 AI、流式对话等多个子系统。本计划只覆盖里程碑 1：基础设施与项目骨架。后续里程碑应分别创建独立实施计划，避免一次性改动过大。

## 文件结构

本计划创建以下文件：

- `README.md`：项目介绍、启动顺序、第一阶段验证命令。
- `.gitignore`：忽略 Java、Python、IDE、Docker 运行产物。
- `pom.xml`：Maven 父工程，统一 Java 版本、Spring Boot、Spring Cloud、Spring Cloud Alibaba 版本。
- `services/gateway-service/pom.xml`：网关服务依赖。
- `services/gateway-service/src/main/java/com/dongyao/myblog/gateway/GatewayApplication.java`：网关启动类。
- `services/gateway-service/src/main/resources/application.yml`：网关端口、Nacos 注册、路由配置。
- `services/blog-service/pom.xml`：博客服务依赖。
- `services/blog-service/src/main/java/com/dongyao/myblog/blog/BlogApplication.java`：博客服务启动类。
- `services/blog-service/src/main/java/com/dongyao/myblog/blog/api/HealthController.java`：健康检查接口。
- `services/blog-service/src/main/resources/application.yml`：博客服务端口、Nacos、MySQL、Redis 基础配置。
- `services/ai-agent-service/pyproject.toml`：Python 服务依赖与项目元信息。
- `services/ai-agent-service/app/main.py`：FastAPI 入口与健康检查接口。
- `services/ai-agent-service/app/core/settings.py`：环境变量配置。
- `services/ai-agent-service/app/api/health.py`：AI 服务健康检查路由。
- `services/ai-agent-service/.env.example`：Python 服务环境变量示例。
- `deploy/docker-compose.yml`：Nacos、MySQL、Redis、RocketMQ、Elasticsearch 基础设施。
- `deploy/mysql/init/001_schema.sql`：第一阶段最小数据库初始化脚本。

---

### Task 1: 仓库基础文件

**Files:**
- Create: `README.md`
- Create: `.gitignore`

- [ ] **Step 1: 创建 README**

写入 `README.md`：

```markdown
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
```

- [ ] **Step 2: 创建 .gitignore**

写入 `.gitignore`：

```gitignore
.idea/
.vscode/
target/
*.iml

__pycache__/
.pytest_cache/
.ruff_cache/
.venv/
venv/
*.pyc

.env
*.log

deploy/data/
```

- [ ] **Step 3: 验证文件存在**

Run:

```powershell
Test-Path README.md; Test-Path .gitignore
```

Expected:

```text
True
True
```

- [ ] **Step 4: 提交**

```powershell
git add README.md .gitignore
git commit -m "docs: add project readme and gitignore"
```

---

### Task 2: Docker Compose 基础设施

**Files:**
- Create: `deploy/docker-compose.yml`
- Create: `deploy/mysql/init/001_schema.sql`

- [ ] **Step 1: 创建 MySQL 初始化脚本**

写入 `deploy/mysql/init/001_schema.sql`：

```sql
CREATE DATABASE IF NOT EXISTS myblog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE myblog;

CREATE TABLE IF NOT EXISTS health_check_marker (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO health_check_marker(name)
SELECT 'bootstrap'
WHERE NOT EXISTS (SELECT 1 FROM health_check_marker WHERE name = 'bootstrap');
```

- [ ] **Step 2: 创建 Docker Compose**

写入 `deploy/docker-compose.yml`：

```yaml
services:
  nacos:
    image: nacos/nacos-server:v2.3.2
    container_name: myblog-nacos
    environment:
      MODE: standalone
      NACOS_AUTH_ENABLE: "false"
    ports:
      - "8848:8848"
      - "9848:9848"

  mysql:
    image: mysql:8.4
    container_name: myblog-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: myblog
      MYSQL_USER: myblog
      MYSQL_PASSWORD: myblog
    ports:
      - "3306:3306"
    volumes:
      - ./mysql/init:/docker-entrypoint-initdb.d:ro

  redis:
    image: redis:7.2
    container_name: myblog-redis
    ports:
      - "6379:6379"

  rocketmq-namesrv:
    image: apache/rocketmq:5.2.0
    container_name: myblog-rocketmq-namesrv
    command: sh mqnamesrv
    ports:
      - "9876:9876"

  rocketmq-broker:
    image: apache/rocketmq:5.2.0
    container_name: myblog-rocketmq-broker
    command: sh mqbroker -n rocketmq-namesrv:9876
    depends_on:
      - rocketmq-namesrv
    environment:
      NAMESRV_ADDR: rocketmq-namesrv:9876
    ports:
      - "10911:10911"
      - "10909:10909"

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.13.4
    container_name: myblog-elasticsearch
    environment:
      discovery.type: single-node
      xpack.security.enabled: "false"
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
```

- [ ] **Step 3: 校验 Compose 配置**

Run:

```powershell
docker compose -f deploy/docker-compose.yml config
```

Expected: 命令退出码为 `0`，输出包含 `myblog-nacos`、`myblog-mysql`、`myblog-redis`。

- [ ] **Step 4: 提交**

```powershell
git add deploy/docker-compose.yml deploy/mysql/init/001_schema.sql
git commit -m "chore: add local infrastructure compose"
```

---

### Task 3: Maven 父工程

**Files:**
- Create: `pom.xml`

- [ ] **Step 1: 创建 Maven 父工程**

写入根目录 `pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.dongyao.myblog</groupId>
    <artifactId>myblog-ai-cloud</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>services/gateway-service</module>
        <module>services/blog-service</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.3.1</spring-boot.version>
        <spring-cloud.version>2023.0.2</spring-cloud.version>
        <spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **Step 2: 验证父工程可被 Maven 解析**

Run:

```powershell
mvn -q help:effective-pom -DskipTests
```

Expected: 当前还没有子模块文件时可能失败，失败信息应指向缺少 `services/gateway-service` 或 `services/blog-service`。这是预期的中间状态。

- [ ] **Step 3: 暂不提交**

父工程需要和两个 Java 子模块一起提交，避免主分支停留在不可解析状态。

---

### Task 4: Gateway Service 骨架

**Files:**
- Create: `services/gateway-service/pom.xml`
- Create: `services/gateway-service/src/main/java/com/dongyao/myblog/gateway/GatewayApplication.java`
- Create: `services/gateway-service/src/main/resources/application.yml`

- [ ] **Step 1: 创建 Gateway POM**

写入 `services/gateway-service/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.dongyao.myblog</groupId>
        <artifactId>myblog-ai-cloud</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>gateway-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建启动类**

写入 `services/gateway-service/src/main/java/com/dongyao/myblog/gateway/GatewayApplication.java`：

```java
package com.dongyao.myblog.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建网关配置**

写入 `services/gateway-service/src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
    gateway:
      routes:
        - id: blog-service
          uri: lb://blog-service
          predicates:
            - Path=/api/blog/**
        - id: ai-agent-service
          uri: http://localhost:8000
          predicates:
            - Path=/api/ai/**

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

- [ ] **Step 4: 暂不提交**

Gateway 需要和 Blog Service 一起通过 Maven 编译后提交。

---

### Task 5: Blog Service 骨架

**Files:**
- Create: `services/blog-service/pom.xml`
- Create: `services/blog-service/src/main/java/com/dongyao/myblog/blog/BlogApplication.java`
- Create: `services/blog-service/src/main/java/com/dongyao/myblog/blog/api/HealthController.java`
- Create: `services/blog-service/src/main/resources/application.yml`

- [ ] **Step 1: 创建 Blog POM**

写入 `services/blog-service/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.dongyao.myblog</groupId>
        <artifactId>myblog-ai-cloud</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>blog-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建启动类**

写入 `services/blog-service/src/main/java/com/dongyao/myblog/blog/BlogApplication.java`：

```java
package com.dongyao.myblog.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建健康检查接口**

写入 `services/blog-service/src/main/java/com/dongyao/myblog/blog/api/HealthController.java`：

```java
package com.dongyao.myblog.blog.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blog")
public class HealthController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "blog-service", "status", "UP");
    }
}
```

- [ ] **Step 4: 创建 Blog 配置**

写入 `services/blog-service/src/main/resources/application.yml`：

```yaml
server:
  port: 8081

spring:
  application:
    name: blog-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
  datasource:
    url: jdbc:mysql://localhost:3306/myblog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: myblog
    password: myblog
  data:
    redis:
      host: localhost
      port: 6379

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

- [ ] **Step 5: 编译 Java 多模块**

Run:

```powershell
mvn -q -DskipTests package
```

Expected: 命令退出码为 `0`，生成 `services/gateway-service/target` 与 `services/blog-service/target`。

- [ ] **Step 6: 提交 Java 骨架**

```powershell
git add pom.xml services/gateway-service services/blog-service
git commit -m "feat: add java service skeletons"
```

---

### Task 6: AI Agent Service 骨架

**Files:**
- Create: `services/ai-agent-service/pyproject.toml`
- Create: `services/ai-agent-service/app/main.py`
- Create: `services/ai-agent-service/app/core/settings.py`
- Create: `services/ai-agent-service/app/api/health.py`
- Create: `services/ai-agent-service/app/__init__.py`
- Create: `services/ai-agent-service/app/api/__init__.py`
- Create: `services/ai-agent-service/app/core/__init__.py`
- Create: `services/ai-agent-service/.env.example`

- [ ] **Step 1: 创建 Python 项目配置**

写入 `services/ai-agent-service/pyproject.toml`：

```toml
[project]
name = "myblog-ai-agent-service"
version = "0.1.0"
description = "FastAPI AI Agent service for MyBlog AI Cloud"
requires-python = ">=3.12"
dependencies = [
  "fastapi>=0.111.0",
  "uvicorn[standard]>=0.30.0",
  "pydantic-settings>=2.3.0",
  "httpx>=0.27.0"
]

[tool.uvicorn]
factory = false
```

- [ ] **Step 2: 创建配置模块**

写入 `services/ai-agent-service/app/core/settings.py`：

```python
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    service_name: str = "ai-agent-service"
    blog_service_base_url: str = "http://localhost:8081"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


settings = Settings()
```

- [ ] **Step 3: 创建健康检查路由**

写入 `services/ai-agent-service/app/api/health.py`：

```python
from fastapi import APIRouter

from app.core.settings import settings

router = APIRouter()


@router.get("/health")
def health() -> dict[str, str]:
    return {"service": settings.service_name, "status": "UP"}
```

- [ ] **Step 4: 创建 FastAPI 入口**

写入 `services/ai-agent-service/app/main.py`：

```python
from fastapi import FastAPI

from app.api.health import router as health_router
from app.core.settings import settings

app = FastAPI(title=settings.service_name)
app.include_router(health_router)
```

- [ ] **Step 5: 创建包标记文件与环境变量示例**

写入空文件：

```text
services/ai-agent-service/app/__init__.py
services/ai-agent-service/app/api/__init__.py
services/ai-agent-service/app/core/__init__.py
```

写入 `services/ai-agent-service/.env.example`：

```dotenv
SERVICE_NAME=ai-agent-service
BLOG_SERVICE_BASE_URL=http://localhost:8081
```

- [ ] **Step 6: 验证 Python 服务可导入**

Run:

```powershell
cd services/ai-agent-service
python -m compileall app
```

Expected: 命令退出码为 `0`，输出包含 `Listing 'app'...`。

- [ ] **Step 7: 提交 Python 骨架**

```powershell
git add services/ai-agent-service
git commit -m "feat: add ai agent service skeleton"
```

---

### Task 7: 第一阶段联调验证

**Files:**
- Modify: `README.md`

- [ ] **Step 1: 启动基础设施**

Run:

```powershell
docker compose -f deploy/docker-compose.yml up -d
docker compose -f deploy/docker-compose.yml ps
```

Expected: `nacos`、`mysql`、`redis`、`rocketmq-namesrv`、`rocketmq-broker`、`elasticsearch` 均为 running 或 healthy。

- [ ] **Step 2: 启动 Blog Service**

Run:

```powershell
mvn -pl services/blog-service spring-boot:run
```

Expected: 日志包含 `Started BlogApplication`。

- [ ] **Step 3: 启动 Gateway Service**

Run:

```powershell
mvn -pl services/gateway-service spring-boot:run
```

Expected: 日志包含 `Started GatewayApplication`。

- [ ] **Step 4: 启动 AI Agent Service**

Run:

```powershell
cd services/ai-agent-service
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Expected: 日志包含 `Uvicorn running on http://0.0.0.0:8000`。

- [ ] **Step 5: 验证健康检查**

Run:

```powershell
curl.exe -m 10 http://localhost:8080/api/blog/health
curl.exe -m 10 http://localhost:8000/health
```

Expected:

```json
{"service":"blog-service","status":"UP"}
{"service":"ai-agent-service","status":"UP"}
```

- [ ] **Step 6: 补充 README 验证结果**

在 `README.md` 追加：

```markdown
## 第一阶段验收

- `docker compose -f deploy/docker-compose.yml config` 可以通过。
- `mvn -q -DskipTests package` 可以通过。
- `python -m compileall app` 可以通过。
- Blog Service 健康检查：`GET http://localhost:8080/api/blog/health`。
- AI Agent Service 健康检查：`GET http://localhost:8000/health`。
```

- [ ] **Step 7: 提交联调文档**

```powershell
git add README.md
git commit -m "docs: document milestone one verification"
```

---

## 自检清单

- 设计文档中的“里程碑 1：基础设施与项目骨架”已被 Task 1 到 Task 7 覆盖。
- 本计划不实现用户鉴权、文章 CRUD、搜索、AI 摘要或 SSE，对应后续里程碑。
- 所有新增文件路径已明确列出。
- 每个实现步骤都给出了具体内容或命令。
- 每个可验证步骤都给出了预期输出。
- 每个任务都包含独立提交点。
