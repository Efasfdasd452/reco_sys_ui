
● 配置完成。以后的工作流程是：

开发时（不变，继续用两个服务器）：

# 后端                                                                                                                              mvn spring-boot:run

# 前端（另一个终端）

```
cd frontend && npm run dev   # 访问 http://localhost:3000
```


演示/部署时（打包到 Spring Boot 统一托管）：
```
cd frontend
npm run build         # 输出到 ../src/main/resources/static/
cd ..
mvn spring-boot:run   # 只需要访问 http://localhost:8888
```

打包后 Spring Boot 同时托管前端和后端，任何路由（如 /student/recommend）刷新都不会 404，因为 SPA fallback 会返回 index.html。     

