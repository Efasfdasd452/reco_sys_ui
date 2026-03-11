# 习题推荐系统（reco_sys）

基于知识图谱与学习状态的**个性化习题推荐系统**，支持学生做题、教师管理、管理员初始化数据。后端整合 MySQL（业务数据）、Neo4j（知识图谱）与独立 Python 推荐服务，前端为 React 单页应用，支持中英文与滑块验证码登录。

## 功能概览

| 角色 | 主要功能 |
|------|----------|
| **学生** | 选课、做题、查看推荐习题、知识图谱、答题历史、个人资料、加入班级 |
| **教师** | 班级/课堂管理、习题与知识点管理、批改、查看学生推荐与知识图谱 |
| **管理员** | 用户管理、数据初始化（课程/知识点/习题）、Neo4j 同步与清理 |

## 技术栈

| 层级 | 技术 | 版本/说明 |
|------|------|-----------|
| 后端 | Java / Spring Boot | 17 / 4.0.3 |
| 后端 | MySQL | 8.0+，业务数据（用户、课程、班级、习题元数据、答题记录等） |
| 后端 | Neo4j | 5.x，知识图谱（知识点、习题节点及关系） |
| 后端 | Spring Security + JWT | 认证与鉴权 |
| 后端 | 外部推荐服务 | Python FastAPI，端口 8000，个性化推荐算法 |
| 前端 | React + Vite | 19.x / 7.x |
| 前端 | Ant Design、React Router、Axios | UI、路由、请求 |
| 前端 | @antv/g6、KaTeX、i18next | 知识图谱可视化、数学公式、中英文 |

## 项目结构

```
reco_sys/
├── src/
│   ├── main/
│   │   ├── java/org/reco/reco_sys/
│   │   │   ├── RecoSysApplication.java    # 启动类
│   │   │   ├── config/                    # 安全、SPA 回退、配置绑定
│   │   │   └── module/
│   │   │       ├── auth/                  # 登录注册、滑块验证码
│   │   │       ├── admin/                 # 管理员、数据初始化
│   │   │       ├── course/                # 课程
│   │   │       ├── classroom/             # 班级/课堂
│   │   │       ├── exercise/              # 习题（MySQL + Neo4j）
│   │   │       ├── knowledge/             # 知识图谱
│   │   │       ├── learning/              # 答题记录、用户-知识点状态
│   │   │       ├── grade/                 # 成绩/批改
│   │   │       ├── recommendation/        # 调用 Python 推荐服务
│   │   │       ├── file/                  # 文件/图片上传
│   │   │       ├── user/                  # 用户信息
│   │   │       ├── notification/          # 通知
│   │   │       └── statistics/            # 统计（管理员）
│   │   └── resources/
│   │       ├── application.properties.example  # 配置模板
│   │       ├── application.properties          # 本地配置（需自行复制填写）
│   │       └── static/                         # 前端构建产物（npm run build 输出）
│   └── test/                                  # 单元测试
├── frontend/                                  # React 前端
│   ├── src/
│   │   ├── App.jsx, main.jsx
│   │   ├── api/                           # 后端 API 封装
│   │   ├── store/                         # 登录状态（authStore）
│   │   ├── components/                   # 布局、验证码、公式渲染等
│   │   ├── pages/                        # 学生/教师/管理员页面
│   │   └── i18n/                         # 中英文
│   ├── vite.config.js                    # 构建输出到 ../src/main/resources/static，dev 代理 /api -> 8888
│   └── package.json
├── sql/                                     # SQL 及数据脚本（如 inject_test_kc_states.py）
├── pom.xml
└── README.md
```

## 快速开始

### 1. 配置文件

```bash
cp src/main/resources/application.properties.example \
   src/main/resources/application.properties
```

编辑 `application.properties`，至少配置：

```properties
# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/reco_sys?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD

# Neo4j
spring.neo4j.uri=neo4j://localhost:7687
spring.neo4j.authentication.username=neo4j
spring.neo4j.authentication.password=YOUR_NEO4J_PASSWORD

# JWT（建议：openssl rand -hex 32）
jwt.secret=YOUR_JWT_SECRET_KEY_AT_LEAST_256_BITS
jwt.expiration=86400000

# Python 推荐服务
recommend.service.url=http://localhost:8000
recommend.service.api-key=YOUR_FASTAPI_API_KEY
```

可选：邮件、文件上传目录、推荐数据路径（如 algebra2005）等，见 `application.properties.example`。

### 2. 创建数据库

```sql
CREATE DATABASE reco_sys CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 开发环境运行（前后端分离，两个进程）

**终端一（后端）：**

```bash
mvn spring-boot:run
```

服务地址：`http://localhost:8888`

**终端二（前端）：**

```bash
cd frontend && npm install && npm run dev
```

前端地址：`http://localhost:3000`（Vite 已配置 `/api` 代理到 8888）

### 4. 演示/部署运行（前后端一体）

前端构建到 Spring Boot 静态目录，只启动一个进程：

```bash
cd frontend
npm install && npm run build    # 输出到 ../src/main/resources/static/
cd ..
mvn spring-boot:run
```

浏览器访问：`http://localhost:8888`。所有非 API、非静态资源的 GET 请求会返回 `index.html`，由 React Router 做 SPA 路由，刷新不 404。

---

## 标准部署（仅后端 JAR）

### 环境依赖

- JDK 17+
- MySQL 8.0+
- Neo4j 5.x
- （可选）Python 推荐服务，需单独启动在 8000 端口

### 步骤

```bash
# 1. 前端先构建（若需一起部署）
cd frontend && npm run build && cd ..

# 2. 打包
./mvnw clean package -DskipTests

# 3. 运行
java -jar target/reco_sys-0.0.1-SNAPSHOT.jar

# 或后台
nohup java -jar target/reco_sys-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

---

## Docker 部署

### 前置要求

- Docker 20.10+
- Docker Compose 2.x

### 构建镜像

```bash
./mvnw clean package -DskipTests
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

### Docker Compose 一键启动

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
      - "7474:7474"
      - "7687:7687"
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
      RECOMMEND_SERVICE_API_KEY: your_api_key

volumes:
  mysql_data:
  neo4j_data:
```

```bash
docker compose up -d
docker compose logs -f app
```

> AI 推荐服务（Python FastAPI）为独立服务，需在宿主机或单独容器中启动，默认 8000 端口。

---

## API 概览

| 路径前缀 | 说明 |
|----------|------|
| `/api/auth` | 注册、登录、邮箱验证码、重置密码 |
| `/api/auth/captcha` | 滑块验证码（获取/校验） |
| `/api/user` | 当前用户资料、登录记录 |
| `/api/courses` | 课程列表、详情、选课、我的课程、课程学生 |
| `/api/classrooms` | 班级 CRUD、加入班级、班级课程、 enroll 学生 |
| `/api/knowledge` | 知识图谱（按课程、按用户状态） |
| `/api/exercises` | 习题列表、详情、按课程查询 |
| `/api/learning` | 提交答案、答题历史、已答题目 ID |
| `/api/grade` | 待批改列表、批改提交 |
| `/api/recommend` | 学生推荐、教师查看某学生推荐、刷新推荐 |
| `/api/files` | 图片上传、移动端上传会话 |
| `/api/notifications` | 通知列表、未读数量 |
| `/api/admin` | 用户列表、数据初始化、同步/清理 Neo4j |
| `/api/admin/statistics` | 管理员统计 |

---

## 相关仓库

- **前端**：本仓库内 `frontend/` 目录。
- **AI 推荐算法服务**（Python FastAPI）：独立仓库，需单独部署并配置 `recommend.service.url` 与 `recommend.service.api-key`。
