package org.reco.reco_sys.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SPA fallback：所有非 API、非静态资源的 GET 请求一律返回 index.html，
 * 由 React Router 在客户端处理路由。
 */
@RestController
public class RootController {

    private final Resource indexHtml = new ClassPathResource("static/index.html");

    @GetMapping(value = {"/", "/{path:[^\\.]*}", "/{path:[^\\.]*}/**"})
    public ResponseEntity<Resource> spa(HttpServletRequest request) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(indexHtml);
    }
}
