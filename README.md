# 习题推荐系统 —— 后端服务

本项目为习题推荐系统的后端服务，基于 Spring Boot 构建，提供 RESTful API 接口。系统通过整合 MySQL 业务数据库与 Neo4j 知识图谱数据库，结合外部 AI 推荐算法服务，实现对学生学习状态的建模与个性化习题推荐。

## 技术栈

| 组件 | 版本 |
|------|------|
| Java | 17+ |
| Spring Boot | 4.0.3 |
| MySQL | 8.0+ |
| Neo4j | 5.x |
| Maven | 3.9+ |

## 项目结构

```
reco_sys/
├── src/
│   ├── main/
│   │   ├── java/org/reco/reco_sys/   # 业务代码
│   │   └── resources/
│   │       ├── application.properties.example  # 配置模板（需复制并填写）
│   │       └── application.properties          # 本地配置（已被 .gitignore 排除）
│   └── test/                          # 单元测试
├── pom.xml
└── .gitignore
```

## 快速开始

### 1. 准备配置文件

```bash
cp src/main/resources/application.properties.example \
   src/main/resources/application.properties
```

编辑 `application.properties`，填写以下配置项：

```properties
# MySQL 数据库连接
spring.datasource.url=jdbc:mysql://localhost:3306/reco_sys?...
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

# Neo4j 知识图谱数据库
spring.neo4j.uri=neo4j://localhost:7687
spring.neo4j.authentication.username=neo4j
spring.neo4j.authentication.password=YOUR_PASSWORD

# JWT 密钥（建议使用 openssl rand -hex 32 生成）
jwt.secret=YOUR_SECRET_KEY

# AI 推荐服务地址
recommend.service.url=http://localhost:8000
recommend.service.api-key=YOUR_API_KEY
```

### 2. 创建 MySQL 数据库

```sql
CREATE DATABASE reco_sys CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 构建与运行

```bash
# 使用 Maven Wrapper 构建
./mvnw clean package -DskipTests

# 运行
java -jar target/reco_sys-0.0.1-SNAPSHOT.jar
```

服务启动后访问：`http://localhost:8888`

---

## 标准部署

### 环境依赖

- JDK 17+
- MySQL 8.0+
- Neo4j 5.x
- AI 推荐服务（独立 Python 服务，需单独启动）

### 部署步骤

```bash
# 1. 克隆项目
git clone git@github.com:Efasfdasd452/reco_sys_ui.git
cd reco_sys_ui

# 2. 配置环境
cp src/main/resources/application.properties.example \
   src/main/resources/application.properties
# 编辑 application.properties，填写数据库密码等信息

# 3. 打包
./mvnw clean package -DskipTests

# 4. 运行
java -jar target/reco_sys-0.0.1-SNAPSHOT.jar

# 或后台运行
nohup java -jar target/reco_sys-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

---

## Docker 部署

### 前置要求

- Docker 20.10+
- Docker Compose 2.x

### 单独构建镜像

```bash
# 先打包
./mvnw clean package -DskipTests

# 构建镜像
docker build -t reco_sys:latest .
```

项目根目录需提供 `Dockerfile`（示例）：

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/reco_sys-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 使用 Docker Compose 一键启动（推荐）

在项目根目录创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: your_password
      MYSQL_DATABASE: reco_sys
      MYSQL_CHARSET: utf8mb4
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  neo4j:
    image: neo4j:5
    environment:
      NEO4J_AUTH: neo4j/your_password
    ports:
      - "7474:7474"   # Web 控制台
      - "7687:7687"   # Bolt 协议
    volumes:
      - neo4j_data:/data

  app:
    image: reco_sys:latest
    depends_on:
      - mysql
      - neo4j
    ports:
      - "8888:8888"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/reco_sys?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_PASSWORD: your_password
      SPRING_NEO4J_URI: neo4j://neo4j:7687
      SPRING_NEO4J_AUTHENTICATION_PASSWORD: your_password
      JWT_SECRET: your_jwt_secret
      RECOMMEND_SERVICE_URL: http://host.docker.internal:8000
      RECOMMEND_SERVICE_API-KEY: your_api_key

volumes:
  mysql_data:
  neo4j_data:
```

```bash
# 启动所有服务
docker compose up -d

# 查看日志
docker compose logs -f app

# 停止
docker compose down
```

> **注意**：AI 推荐服务（Python FastAPI）为独立服务，需在宿主机或单独容器中启动，默认监听 `8000` 端口。

---

## API 文档

服务启动后，接口按以下路径前缀组织：

| 路径前缀 | 说明 |
|----------|------|
| `/api/auth/**` | 用户认证（注册/登录/Token 刷新） |
| `/api/users/**` | 用户信息管理 |
| `/api/courses/**` | 课程管理 |
| `/api/knowledge/**` | 知识图谱操作 |
| `/api/exercises/**` | 习题管理 |
| `/api/learning/**` | 答题记录提交与查询 |
| `/api/assessment/**` | 学习状态评估 |
| `/api/recommendations/**` | 个性化习题推荐 |
| `/api/files/**` | 图片等文件上传与访问 |

---

## 相关仓库

- 前端（React）：待补充
- AI 推荐算法服务（Python FastAPI）：私有仓库
