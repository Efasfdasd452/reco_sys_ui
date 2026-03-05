package org.reco.reco_sys.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
public class AppProperties {

    // Python 推荐服务配置（从 application.properties 读取）
    @org.springframework.beans.factory.annotation.Value("${recommend.service.url}")
    private String recommendServiceUrl;

    @org.springframework.beans.factory.annotation.Value("${recommend.service.api-key}")
    private String recommendServiceApiKey;

    // 服务器对外访问地址（部署时配置，本地开发时自动检测局域网IP）
    @org.springframework.beans.factory.annotation.Value("${app.base-url:}")
    private String baseUrl;

    // 文件上传根目录
    @org.springframework.beans.factory.annotation.Value("${app.upload-dir:uploads}")
    private String uploadDir;
}
