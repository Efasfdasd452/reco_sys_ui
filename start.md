# 开发工作流备忘

## 日常开发（前后端分离，两个进程）

```bash
# 终端一：后端
mvn spring-boot:run
# 访问 http://localhost:8888

# 终端二：前端
cd frontend && npm run dev
# 访问 http://localhost:3000（/api 自动代理到 8888）
```

## 演示 / 答辩（前后端一体，单进程）

```bash
cd frontend
npm run build         # 构建到 ../src/main/resources/static/
cd ..
mvn spring-boot:run   # 只需访问 http://localhost:8888
```

打包后 Spring Boot 同时托管前端和后端，任何路由（如 `/student/recommend`）刷新都不会 404，SPA fallback 会返回 `index.html`。

## 打包 JAR

```bash
cd frontend && npm run build && cd ..
./mvnw clean package -DskipTests
# 产物：target/reco_sys-0.0.1-SNAPSHOT.jar
java -jar target/reco_sys-0.0.1-SNAPSHOT.jar
```

## 依赖服务

| 服务 | 地址 | 说明 |
|------|------|------|
| MySQL | 192.168.50.129:3306/reco_sys | 业务数据 |
| Neo4j | 127.0.0.1:7687 | 知识图谱 |
| Python 推荐服务 | http://localhost:8000 | FastAPI，需单独启动 |

## 注意事项

- `application.properties` 已在 `.gitignore` 中忽略，不会提交
- `app.totp.encryption-key` 是 TOTP 密钥的 AES-256-GCM 加密密钥，不得泄露
- 生产环境建议通过环境变量注入敏感配置：
  ```bash
  export TOTP_ENCRYPTION_KEY="..."
  java -jar reco_sys.jar
  ```
