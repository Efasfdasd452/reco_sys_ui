终端 1 — Spring Boot 后端：     
```
cd C:\Users\ASUS\important_files\java_local\reco_sys                                                                              
./mvnw spring-boot:run                                                                                                               
```

终端 2 — React 前端：                                                                                                             

```
cd C:\Users\ASUS\important_files\java_local\reco_sys\frontend                    
npm run dev
```

然后访问 http://localhost:3000。

  ---

前端 3000 端口会自动将所有 /api/* 请求代理到后端 8888，所以跨域问题不存在。访问 8888 端口只是纯 API，前端入口是 3000。

