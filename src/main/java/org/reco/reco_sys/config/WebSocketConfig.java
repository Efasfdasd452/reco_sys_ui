package org.reco.reco_sys.config;

import org.reco.reco_sys.module.websocket.handler.MobileUploadWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(mobileUploadWebSocketHandler(), "/ws/upload/{token}")
                .setAllowedOriginPatterns("*");
    }

    @Bean
    public MobileUploadWebSocketHandler mobileUploadWebSocketHandler() {
        return new MobileUploadWebSocketHandler();
    }
}
